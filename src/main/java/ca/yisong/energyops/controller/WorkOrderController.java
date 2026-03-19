package ca.yisong.energyops.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.yisong.energyops.api.ApiModels.WorkOrderCreateRequest;
import ca.yisong.energyops.api.ApiModels.WorkOrderResponse;
import ca.yisong.energyops.api.ApiModels.WorkOrderUpdateRequest;
import ca.yisong.energyops.service.WorkOrderService;
import ca.yisong.energyops.support.SecurityUtils;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public List<WorkOrderResponse> listWorkOrders() {
        return workOrderService.listWorkOrders();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_ENGINEER')")
    public WorkOrderResponse createWorkOrder(@Valid @RequestBody WorkOrderCreateRequest request) {
        return workOrderService.createWorkOrder(request, SecurityUtils.currentUsername());
    }

    @PatchMapping("/{workOrderId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_ENGINEER','TECHNICIAN')")
    public WorkOrderResponse updateWorkOrder(@PathVariable Long workOrderId, @RequestBody WorkOrderUpdateRequest request) {
        return workOrderService.updateWorkOrder(workOrderId, request, SecurityUtils.currentUsername());
    }
}
