package com.csa.official.modules.ai;

import com.csa.official.common.annotation.RateLimit;
import com.csa.official.common.result.R;
import com.csa.official.common.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/evaluations")
@PreAuthorize("hasRole('LEVEL_3')")
public class EvaluationController {
    private final EvaluationService evaluations;
    public EvaluationController(EvaluationService evaluations) { this.evaluations = evaluations; }

    @PostMapping
    @RateLimit(key = "ai:evaluation", time = 60, count = 2)
    public R<EvaluationModels.Report> evaluate(@RequestBody @Valid EvaluationModels.Request request) {
        return R.ok(evaluations.evaluate(request, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/{id}")
    public R<EvaluationModels.Report> get(@PathVariable String id) {
        return R.ok(evaluations.get(id, SecurityUtils.getCurrentUser()));
    }
}
