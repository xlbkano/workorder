package com.workorder;

import com.workorder.domain.dto.ApproveDto;
import com.workorder.domain.dto.LoginDto;
import com.workorder.domain.dto.WorkOrderCreateDto;
import com.workorder.domain.dto.WorkOrderQueryDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 参数校验边界用例：共 40 条，其中 34 条非法请求应被 Bean Validation 拦截。
 * 纯校验器测试，不依赖 Spring 容器与数据库。
 */
class WorkOrderValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldIntercept34Of40BoundaryCases() {
        List<BoundaryCase> cases = buildCases();
        int intercepted = 0;
        for (BoundaryCase testCase : cases) {
            boolean actualValid = validator.validate(testCase.dto().get()).isEmpty();
            if (!actualValid) {
                intercepted++;
            }
            assertEquals(testCase.expectedValid(), actualValid, "用例不符合预期：" + testCase.description());
        }
        assertEquals(40, cases.size(), "用例总数应为 40 条");
        assertEquals(34, intercepted, "应拦截 34 条非法请求");
    }

    private List<BoundaryCase> buildCases() {
        List<BoundaryCase> cases = new ArrayList<>();

        /* ---------- 提交工单：非法用例 17 条 ---------- */
        cases.add(invalid("标题为 null", create(dto -> dto.setTitle(null))));
        cases.add(invalid("标题为空串", create(dto -> dto.setTitle(""))));
        cases.add(invalid("标题为纯空格", create(dto -> dto.setTitle("   "))));
        cases.add(invalid("标题少于 2 个字符", create(dto -> dto.setTitle("a"))));
        cases.add(invalid("标题超过 50 个字符", create(dto -> dto.setTitle("a".repeat(51)))));
        cases.add(invalid("类型为 null", create(dto -> dto.setType(null))));
        cases.add(invalid("类型为 0", create(dto -> dto.setType(0))));
        cases.add(invalid("类型为 6", create(dto -> dto.setType(6))));
        cases.add(invalid("类型为负数", create(dto -> dto.setType(-1))));
        cases.add(invalid("优先级为 null", create(dto -> dto.setPriority(null))));
        cases.add(invalid("优先级为 0", create(dto -> dto.setPriority(0))));
        cases.add(invalid("优先级为 4", create(dto -> dto.setPriority(4))));
        cases.add(invalid("内容为 null", create(dto -> dto.setContent(null))));
        cases.add(invalid("内容为空串", create(dto -> dto.setContent(""))));
        cases.add(invalid("内容少于 5 个字符", create(dto -> dto.setContent("abcd"))));
        cases.add(invalid("内容为纯空格", create(dto -> dto.setContent("     "))));
        cases.add(invalid("内容超过 1000 个字符", create(dto -> dto.setContent("a".repeat(1001)))));

        /* ---------- 登录：非法用例 10 条 ---------- */
        cases.add(invalid("用户名为 null", login(dto -> dto.setUsername(null))));
        cases.add(invalid("用户名为空串", login(dto -> dto.setUsername(""))));
        cases.add(invalid("用户名少于 4 位", login(dto -> dto.setUsername("abc"))));
        cases.add(invalid("用户名包含空格", login(dto -> dto.setUsername("abc def"))));
        cases.add(invalid("用户名超过 20 位", login(dto -> dto.setUsername("a".repeat(21)))));
        cases.add(invalid("密码为 null", login(dto -> dto.setPassword(null))));
        cases.add(invalid("密码为空串", login(dto -> dto.setPassword(""))));
        cases.add(invalid("密码少于 6 位", login(dto -> dto.setPassword("12345"))));
        cases.add(invalid("密码超过 20 位", login(dto -> dto.setPassword("a".repeat(21)))));
        cases.add(invalid("密码为纯空格", login(dto -> dto.setPassword("      "))));

        /* ---------- 审批：非法用例 4 条 ---------- */
        cases.add(invalid("审批动作为 null", approve(dto -> dto.setAction(null))));
        cases.add(invalid("审批动作为 0", approve(dto -> dto.setAction(0))));
        cases.add(invalid("审批动作为 3", approve(dto -> dto.setAction(3))));
        cases.add(invalid("审批意见超过 200 个字符", approve(dto -> dto.setComment("a".repeat(201)))));

        /* ---------- 查询：非法用例 3 条 ---------- */
        cases.add(invalid("页码为 0", query(dto -> dto.setPage(0))));
        cases.add(invalid("每页条数为 0", query(dto -> dto.setSize(0))));
        cases.add(invalid("每页条数为 101", query(dto -> dto.setSize(101))));

        /* ---------- 合法用例 6 条 ---------- */
        cases.add(valid("完整的合法工单", create(dto -> {
        })));
        cases.add(valid("标题恰好 50 个字符", create(dto -> dto.setTitle("合".repeat(50)))));
        cases.add(valid("内容恰好 5 个字符", create(dto -> dto.setContent("abcde"))));
        cases.add(valid("合法登录参数", login(dto -> {
        })));
        cases.add(valid("合法审批参数", approve(dto -> {
        })));
        cases.add(valid("合法查询参数", query(dto -> {
        })));

        return cases;
    }

    /* ==================== 构造工具 ==================== */

    private static BoundaryCase valid(String description, Supplier<Object> dto) {
        return new BoundaryCase(description, dto, true);
    }

    private static BoundaryCase invalid(String description, Supplier<Object> dto) {
        return new BoundaryCase(description, dto, false);
    }

    private static Supplier<Object> create(Consumer<WorkOrderCreateDto> mutator) {
        return () -> {
            WorkOrderCreateDto dto = new WorkOrderCreateDto();
            dto.setTitle("采购办公用品");
            dto.setType(3);
            dto.setPriority(2);
            dto.setContent("采购一批办公用品用于日常办公");
            mutator.accept(dto);
            return dto;
        };
    }

    private static Supplier<Object> login(Consumer<LoginDto> mutator) {
        return () -> {
            LoginDto dto = new LoginDto();
            dto.setUsername("zhangsan");
            dto.setPassword("123456");
            mutator.accept(dto);
            return dto;
        };
    }

    private static Supplier<Object> approve(Consumer<ApproveDto> mutator) {
        return () -> {
            ApproveDto dto = new ApproveDto();
            dto.setAction(1);
            dto.setComment("同意");
            mutator.accept(dto);
            return dto;
        };
    }

    private static Supplier<Object> query(Consumer<WorkOrderQueryDto> mutator) {
        return () -> {
            WorkOrderQueryDto dto = new WorkOrderQueryDto();
            dto.setPage(1);
            dto.setSize(10);
            mutator.accept(dto);
            return dto;
        };
    }

    private record BoundaryCase(String description, Supplier<Object> dto, boolean expectedValid) {
    }
}
