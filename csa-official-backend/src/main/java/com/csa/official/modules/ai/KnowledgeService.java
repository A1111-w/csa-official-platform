package com.csa.official.modules.ai;

import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.regex.Pattern;
import static com.csa.official.modules.ai.AiModels.*;

@Service
public class KnowledgeService {
    private final JdbcTemplate db;
    private final ModelGateway model;
    private final AiProperties config;
    private final ObjectMapper json;
    public KnowledgeService(JdbcTemplate db, ModelGateway model, AiProperties config, ObjectMapper json) {
        this.db=db; this.model=model; this.config=config; this.json=json;
    }
    public List<Document> list(int role) {
        return db.query("SELECT id,title,source,min_role_level,chunk_count FROM ai_document WHERE min_role_level<=? ORDER BY created_at DESC,id LIMIT 200",
                (rs,n)->new Document(rs.getString(1),rs.getString(2),rs.getString(3),rs.getInt(4),rs.getInt(5)), role);
    }
    @Transactional
    public Document create(DocumentRequest request, User user) {
        requireRole(user,3);
        if(request.title()==null||request.title().isBlank()||request.title().length()>200||request.content()==null
                ||request.content().isBlank()||request.content().length()>50000||request.minRoleLevel()<1
                ||request.minRoleLevel()>user.getRoleLevel()||request.source()!=null&&request.source().length()>500)
            throw new CsaException(400,"知识文档字段或访问等级不合法");
        String id=UUID.randomUUID().toString();
        List<String> chunks=split(request.content());
        // Explicit capacity boundary: exhaustive scoring is intended for a small demonstration corpus.
        Integer existing=db.queryForObject("SELECT COUNT(*) FROM ai_chunk",Integer.class);
        if(existing!=null&&existing+chunks.size()>2000) throw new CsaException(409,"演示索引最多 2000 个分块，请先清理文档");
        List<double[]> embeddings=config.embeddingsEnabled()?model.embed(chunks):Collections.emptyList();
        String source=request.source()==null?"":request.source().trim();
        db.update("INSERT INTO ai_document(id,title,source,min_role_level,chunk_count,owner_id) VALUES(?,?,?,?,?,?)",
                id,request.title().trim(),source,request.minRoleLevel(),chunks.size(),user.getId());
        for(int i=0;i<chunks.size();i++) {
            String vector=embeddings.isEmpty()?null:encode(embeddings.get(i));
            db.update("INSERT INTO ai_chunk(id,document_id,chunk_index,content,embedding_json,embedding_model) VALUES(?,?,?,?,?,?)",
                    id+":"+i,id,i,chunks.get(i),vector,embeddings.isEmpty()?null:config.getEmbeddingModel());
        }
        return new Document(id,request.title().trim(),source,request.minRoleLevel(),chunks.size());
    }
    @Transactional
    public void delete(String id, User user) {
        requireRole(user,3);
        if(db.update("DELETE FROM ai_document WHERE id=? AND min_role_level<=?",id,user.getRoleLevel())==0)
            throw new CsaException(404,"知识文档不存在");
        db.update("DELETE FROM ai_chunk WHERE document_id=?",id);
    }
    public boolean canRead(String documentId,int role) {
        Integer count=db.queryForObject("SELECT COUNT(*) FROM ai_document WHERE id=? AND min_role_level<=?",Integer.class,documentId,role);
        return count!=null&&count>0;
    }
    public List<Citation> search(String query,int roleLevel,int topK) {
        if(roleLevel<1) throw new CsaException(403,"会员权限不足");
        if(query==null||query.isBlank()||query.length()>2000) throw new CsaException(400,"检索问题长度应为 1 到 2000 字符");
        // ACL belongs inside the candidate query, before BM25 statistics, cosine scoring and top-k.
        List<Chunk> rows=db.query("SELECT c.id,c.document_id,d.title,d.source,c.content,c.embedding_json,c.embedding_model FROM ai_chunk c JOIN ai_document d ON d.id=c.document_id WHERE d.min_role_level<=? ORDER BY c.id LIMIT 2001",
                (rs,n)->new Chunk(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7)),roleLevel);
        if(rows.size()>2000) throw new CsaException(409,"演示索引容量已超过 2000 个分块");
        if(rows.isEmpty()) return List.of();
        List<List<String>> corpus=rows.stream().map(c->tokens(c.title+" "+c.text)).toList();
        List<String> terms=tokens(query).stream().distinct().toList();
        Map<String,Integer> documentFrequency=new HashMap<>();
        List<Map<String,Integer>> frequencies=new ArrayList<>();
        for(List<String> doc:corpus) {
            Map<String,Integer> frequency=new HashMap<>();
            doc.forEach(term->frequency.merge(term,1,Integer::sum));
            frequency.keySet().forEach(term->documentFrequency.merge(term,1,Integer::sum));
            frequencies.add(frequency);
        }
        double avg=corpus.stream().mapToInt(List::size).average().orElse(1);
        Map<String,Double> lexical=new HashMap<>();
        for(int i=0;i<rows.size();i++) {
            List<String> doc=corpus.get(i); double score=0;
            for(String term:terms) {
                int tf=frequencies.get(i).getOrDefault(term,0);
                if(tf==0) continue;
                int df=documentFrequency.get(term);
                double idf=Math.log(1+(rows.size()-df+0.5)/(df+0.5));
                score+=idf*tf*2.2/(tf+1.2*(0.25+0.75*doc.size()/Math.max(1,avg)));
            }
            if(score>0) lexical.put(rows.get(i).id,score);
        }
        Map<String,Double> semantic=new HashMap<>();
        if(config.embeddingsEnabled()&&rows.stream().anyMatch(c->config.getEmbeddingModel().equals(c.embeddingModel))) {
            double[] q=model.embed(List.of(query)).get(0);
            for(Chunk row:rows) if(config.getEmbeddingModel().equals(row.embeddingModel)&&row.vector!=null) {
                try {
                    double score=cosine(q,json.readValue(row.vector,double[].class));
                    if(score>0.15) semantic.put(row.id,score);
                } catch(Exception e) { throw new CsaException(409,"向量索引损坏，请重新导入文档"); }
            }
        }
        Map<String,Double> fusion=new HashMap<>();
        addRanks(lexical,fusion); addRanks(semantic,fusion);
        return rows.stream().filter(c->fusion.containsKey(c.id))
                .sorted(Comparator.<Chunk>comparingDouble(c->fusion.get(c.id)).reversed().thenComparing(c->c.id))
                .limit(Math.max(1,Math.min(10,topK)))
                .map(c->new Citation(c.documentId,c.id,c.title,c.source,c.text,fusion.get(c.id))).toList();
    }
    static List<String> split(String content) {
        String text=content.replace("\r\n","\n").replace('\r','\n').trim();
        List<String> out=new ArrayList<>();
        for(int start=0;start<text.length();) {
            int end=Math.min(start+700,text.length());
            if(end<text.length()&&Character.isHighSurrogate(text.charAt(end-1))) end--;
            out.add(text.substring(start,end));
            if(end==text.length()) break;
            start=end-100;
            if(Character.isLowSurrogate(text.charAt(start))) start++;
        }
        return out;
    }
    static List<String> tokens(String text) {
        List<String> out=new ArrayList<>();
        var matcher=Pattern.compile("[a-z0-9_]+|[\\p{IsHan}]+",Pattern.CASE_INSENSITIVE).matcher(text.toLowerCase(Locale.ROOT));
        while(matcher.find()) {
            String word=matcher.group();
            if(Character.UnicodeScript.of(word.codePointAt(0))==Character.UnicodeScript.HAN) {
                int[] points=word.codePoints().toArray();
                if(points.length==1) out.add(word);
                for(int i=0;i<points.length-1;i++) out.add(new String(points,i,2));
            } else out.add(word);
        }
        return out;
    }
    static double cosine(double[] a,double[] b) {
        if(a.length!=b.length) return 0;
        double dot=0,aa=0,bb=0;
        for(int i=0;i<a.length;i++) { dot+=a[i]*b[i]; aa+=a[i]*a[i]; bb+=b[i]*b[i]; }
        return aa==0||bb==0?0:dot/Math.sqrt(aa*bb);
    }
    private static void addRanks(Map<String,Double> scores,Map<String,Double> fused) {
        List<String> ranking=scores.keySet().stream().sorted(Comparator.<String>comparingDouble(scores::get).reversed().thenComparing(s->s)).toList();
        for(int i=0;i<ranking.size();i++) fused.merge(ranking.get(i),1.0/(61+i),Double::sum);
    }
    static void requireRole(User user,int min) {
        if(user==null||user.getId()==null||user.getRoleLevel()==null||user.getRoleLevel()<min) throw new CsaException(403,"权限不足");
    }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); } catch(Exception e) { throw new IllegalStateException(e); }
    }
    private record Chunk(String id,String documentId,String title,String source,String text,String vector,String embeddingModel) {}
}
