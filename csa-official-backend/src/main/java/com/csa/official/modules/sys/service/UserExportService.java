package com.csa.official.modules.sys.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.AbstractColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.modules.sys.dto.ExportDto;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserExportService {

    @Autowired
    private UserMapper userMapper;

    // 🔒 1. 补全白名单定义
    private static final Set<String> SUPPORTED_COLUMNS = new HashSet<>(Arrays.asList(
            "college", "className", "realName", "studentId", "phone",
            "username", "contact", "merchantNo", "usedInviteCode", "createTime", "roleLevel"));

    @PreAuthorize("hasRole('LEVEL_4') or hasRole('ADMIN')")
    public void exportMembers(@RequestBody ExportDto dto, HttpServletResponse response) throws IOException {
        // === 2. 校验并清洗列名 ===
        List<String> rawColumns = dto.getColumns();
        if (rawColumns == null || rawColumns.isEmpty()) {
            // 默认列
            rawColumns = Arrays.asList("college", "className", "realName", "studentId", "phone");
        }

        // 过滤掉不在白名单里的“脏数据”
        List<String> targetColumns = rawColumns.stream()
                .filter(SUPPORTED_COLUMNS::contains)
                .collect(Collectors.toList());

        if (targetColumns.isEmpty()) {
            throw new RuntimeException("请选择有效的导出列");
        }

        // === 3. 准备文件名 (保持不变) ===
        DateTimeFormatter filenameDtf = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        String timeRangeStr;
        if (dto.getStartTime() != null && dto.getEndTime() != null) {
            timeRangeStr = dto.getStartTime().format(filenameDtf) + "-" + dto.getEndTime().format(filenameDtf);
        } else if (dto.getStartTime() != null) {
            timeRangeStr = dto.getStartTime().format(filenameDtf) + "起";
        } else if (dto.getEndTime() != null) {
            timeRangeStr = "截止" + dto.getEndTime().format(filenameDtf);
        } else {
            timeRangeStr = "全部";
        }
        String rawFileName = "广州华立学院计算机协会" + timeRangeStr + "会员";
        String encodedFileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");

        // === 4. 构建表头 ===
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String bigTitle = "广州华立学院计算机协会人员信息汇总表 (" + dateStr + ")";

        List<List<String>> heads = new ArrayList<>();
        heads.add(Arrays.asList(bigTitle, "序号"));
        for (String field : targetColumns) {
            heads.add(Arrays.asList(bigTitle, getCnName(field)));
        }

        // === 5. 核心查询逻辑 ===
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        if (dto.getRoleLevel() != null)
            query.eq(User::getRoleLevel, dto.getRoleLevel());
        else
            query.ge(User::getRoleLevel, 1);

        query.eq(StringUtils.hasText(dto.getCollege()), User::getCollege, dto.getCollege());
        query.eq(StringUtils.hasText(dto.getClassName()), User::getClassName, dto.getClassName());
        query.eq(StringUtils.hasText(dto.getStudentId()), User::getStudentId, dto.getStudentId());

        // 🔒 2. 补全 inviteCode 筛选逻辑
        query.eq(StringUtils.hasText(dto.getInviteCode()), User::getUsedInviteCode, dto.getInviteCode());

        query.like(StringUtils.hasText(dto.getRealName()), User::getRealName, dto.getRealName());

        if (dto.getStartTime() != null)
            query.ge(User::getCreateTime, dto.getStartTime());
        if (dto.getEndTime() != null)
            query.le(User::getCreateTime, dto.getEndTime());
        query.orderByDesc(User::getCollege).orderByDesc(User::getClassName).orderByDesc(User::getCreateTime);

        // === 6. 分批写入 Excel ===
        try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream())
                .registerWriteHandler(new CustomColumnWidthStrategy())
                .registerWriteHandler(getStyleStrategy())
                .registerWriteHandler(new AutoFilterStrategy())
                .head(heads)
                .build()) {

            WriteSheet writeSheet = EasyExcel.writerSheet("会员名单").build();

            int current = 1;
            int size = 1000;
            int globalIndex = 1;

            while (true) {
                Page<User> pageParam = new Page<>(current, size, false);
                IPage<User> pageResult = userMapper.selectPage(pageParam, query);
                List<User> records = pageResult.getRecords();
                if (records == null || records.isEmpty()) {
                    break;
                }

                List<List<Object>> batchData = new ArrayList<>();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                for (User user : records) {
                    List<Object> row = new ArrayList<>();
                    row.add(globalIndex++);
                    for (String field : targetColumns) {
                        // 3. 调用三参数的 getFieldValue
                        row.add(getFieldValue(user, field, dtf));
                    }
                    batchData.add(row);
                }

                excelWriter.write(batchData, writeSheet);

                if (records.size() < size) {
                    break;
                }
                current++;
            }
        }
    }

    // --- 下面是辅助类和方法 ---

    public static class CustomColumnWidthStrategy extends AbstractColumnWidthStyleStrategy {
        @Override
        protected void setColumnWidth(WriteSheetHolder writeSheetHolder, List<WriteCellData<?>> cellDataList, Cell cell,
                Head head, Integer relativeRowIndex, Boolean isHead) {
            if (isHead) {
                writeSheetHolder.getSheet().setColumnWidth(cell.getColumnIndex(), 35 * 256);
            } else {
                writeSheetHolder.getSheet().setColumnWidth(cell.getColumnIndex(), 30 * 256);
            }
        }
    }

    public static class AutoFilterStrategy implements SheetWriteHandler {
        @Override
        public void afterSheetCreate(WriteWorkbookHolder wbHolder, WriteSheetHolder wsHolder) {
            Sheet sheet = wsHolder.getSheet();
            sheet.setAutoFilter(new CellRangeAddress(1, 1, 0, 100));
        }
    }

    private HorizontalCellStyleStrategy getStyleStrategy() {
        WriteCellStyle headStyle = new WriteCellStyle();
        headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        WriteFont headFont = new WriteFont();
        headFont.setFontName("Microsoft YaHei");
        headFont.setFontHeightInPoints((short) 11);
        headFont.setBold(true);
        headStyle.setWriteFont(headFont);
        headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headStyle.setBorderBottom(BorderStyle.THIN);
        headStyle.setBorderLeft(BorderStyle.THIN);
        headStyle.setBorderRight(BorderStyle.THIN);
        headStyle.setBorderTop(BorderStyle.THIN);

        WriteCellStyle contentStyle = new WriteCellStyle();
        WriteFont contentFont = new WriteFont();
        contentFont.setFontName("Microsoft YaHei");
        contentFont.setFontHeightInPoints((short) 10);
        contentStyle.setWriteFont(contentFont);
        contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);

        return new HorizontalCellStyleStrategy(headStyle, contentStyle);
    }

    private String getCnName(String field) {
        switch (field) {
            case "college":
                return "学院";
            case "className":
                return "班级";
            case "realName":
                return "姓名";
            case "studentId":
                return "学号";
            case "phone":
                return "手机号";
            case "username":
                return "系统账号";
            case "contact":
                return "其他联系方式";
            case "merchantNo":
                return "支付单号";
            case "usedInviteCode":
                return "邀请码";
            case "createTime":
                return "注册时间";
            case "roleLevel":
                return "等级";
            default:
                return field;
        }
    }

    private Object getFieldValue(User user, String field, DateTimeFormatter dtf) {
        switch (field) {
            case "college":
                return user.getCollege();
            case "className":
                return user.getClassName();
            case "realName":
                return user.getRealName();
            case "studentId":
                return user.getStudentId();
            case "phone":
                return user.getPhone();
            case "username":
                return user.getUsername();
            case "contact":
                return user.getContact();
            case "merchantNo":
                return user.getMerchantNo();
            case "usedInviteCode":
                return user.getUsedInviteCode();
            case "createTime":
                return user.getCreateTime() != null ? dtf.format(user.getCreateTime()) : "";
            case "roleLevel":
                return user.getRoleLevel();
            default:
                return "";
        }
    }
}