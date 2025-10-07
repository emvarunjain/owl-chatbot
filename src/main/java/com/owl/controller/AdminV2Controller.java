package com.owl.controller;

import com.owl.model.ConnectorConfig;
import com.owl.model.Plan;
import com.owl.security.TenantAuth;
import com.owl.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v2/admin", "/api/admin/v2"})
public class AdminV2Controller {
    private final PlanService plans;
    private final QuotaService quotas;
    private final ConnectorService connectors;
    private final ModelRoutingService routing;
    private final TenantAuth auth;
    private final EvalService eval;
    private final ModelCredentialsService credentials;

    public AdminV2Controller(PlanService plans, QuotaService quotas, ConnectorService connectors,
                             ModelRoutingService routing, TenantAuth auth, EvalService eval, ModelCredentialsService credentials) {
        this.plans = plans; this.quotas = quotas; this.connectors = connectors; this.routing = routing; this.auth = auth; this.eval = eval; this.credentials = credentials;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<Plan>> listPlans() { return ResponseEntity.ok(plans.list()); }

    public record AssignPlan(String tenantId, String planName) {}
    @PostMapping("/plans/assign")
    public ResponseEntity<Map<String, Object>> assign(@RequestBody AssignPlan req) {
        auth.authorize(req.tenantId());
        plans.assign(req.tenantId(), req.planName());
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    @GetMapping("/connectors")
    public ResponseEntity<List<ConnectorConfig>> listConnectors(@RequestParam String tenantId) {
        auth.authorize(tenantId);
        return ResponseEntity.ok(connectors.list(tenantId));
    }

    public record CreateConnector(String tenantId, String type, Map<String,Object> config) {}
    @PostMapping("/connectors")
    public ResponseEntity<ConnectorConfig> createConnector(@RequestBody CreateConnector req) {
        auth.authorize(req.tenantId());
        return ResponseEntity.ok(connectors.create(req.tenantId(), req.type(), req.config()));
    }

    @DeleteMapping("/connectors/{id}")
    public ResponseEntity<Map<String, Object>> deleteConnector(@RequestParam String tenantId, @PathVariable String id) {
        auth.authorize(tenantId);
        connectors.delete(tenantId, id);
        return ResponseEntity.ok(Map.of("status","deleted"));
    }

    @PostMapping("/connectors/{id}/sync")
    public ResponseEntity<Map<String,Object>> sync(@RequestParam String tenantId, @PathVariable String id) {
        auth.authorize(tenantId);
        connectors.startSync(tenantId, id);
        return ResponseEntity.ok(Map.of("status","started"));
    }

    public record SetRouting(String tenantId, String provider, String chatModel, String embedModel) {}
    @PostMapping("/routing")
    public ResponseEntity<Map<String,Object>> routing(@RequestBody SetRouting req) {
        auth.authorize(req.tenantId());
        routing.setForTenant(req.tenantId(), req.provider(), req.chatModel(), req.embedModel());
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    public record Golden(String question, String mustContain) {}
    public record EvalReq(String tenantId, List<Golden> tests) {}
    @PostMapping("/eval")
    public ResponseEntity<Map<String,Object>> runEval(@RequestBody EvalReq req) {
        auth.authorize(req.tenantId());
        var res = eval.run(req.tenantId(), req.tests().stream().map(g -> new EvalService.Golden(g.question(), g.mustContain())).toList());
        return ResponseEntity.ok(Map.of("total", res.total(), "passed", res.passed(), "failures", res.failures()));
    }

    public record CredentialsReq(String tenantId, String provider, String apiKey, String endpoint, String azureDeployment, String region) {}
    @PostMapping("/credentials")
    public ResponseEntity<Map<String,Object>> setCredentials(@RequestBody CredentialsReq req) {
        auth.authorize(req.tenantId());
        var mc = new com.owl.model.ModelCredentials();
        mc.setTenantId(req.tenantId()); mc.setProvider(req.provider()); mc.setApiKey(req.apiKey()); mc.setEndpoint(req.endpoint()); mc.setAzureDeployment(req.azureDeployment()); mc.setRegion(req.region());
        credentials.set(mc);
        return ResponseEntity.ok(Map.of("status","ok"));
    }
}
