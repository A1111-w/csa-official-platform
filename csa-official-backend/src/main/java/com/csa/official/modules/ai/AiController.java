package com.csa.official.modules.ai;

import com.csa.official.common.annotation.RateLimit;
import com.csa.official.common.result.R;
import com.csa.official.common.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.csa.official.modules.ai.AiModels.*;

@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasRole('LEVEL_1')")
public class AiController {
    private final KnowledgeService knowledge;
    private final AgentService agent;
    public AiController(KnowledgeService knowledge,AgentService agent) { this.knowledge=knowledge; this.agent=agent; }
    @GetMapping("/status") public R<Status> status() { return R.ok(agent.status()); }
    @GetMapping("/documents") public R<List<Document>> documents() { return R.ok(knowledge.list(SecurityUtils.getCurrentUser().getRoleLevel())); }
    @PostMapping("/documents") @PreAuthorize("hasRole('LEVEL_3')")
    @RateLimit(key="ai-index",time=60,count=5)
    public R<Document> create(@Valid @RequestBody DocumentRequest request) { return R.ok(knowledge.create(request,SecurityUtils.getCurrentUser())); }
    @DeleteMapping("/documents/{id}") @PreAuthorize("hasRole('LEVEL_3')")
    public R<String> delete(@PathVariable String id) { knowledge.delete(id,SecurityUtils.getCurrentUser()); return R.ok("已删除"); }
    @PostMapping("/runs") @RateLimit(key="ai-run",time=60,count=10)
    public R<RunResult> run(@Valid @RequestBody RunRequest request) { return R.ok(agent.run(request.question(),SecurityUtils.getCurrentUser())); }
    @GetMapping("/runs/{id}") public R<RunResult> get(@PathVariable String id) { return R.ok(agent.get(id,SecurityUtils.getCurrentUser())); }
}
