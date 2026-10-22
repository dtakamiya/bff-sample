package com.example.bff.webclient.web

import com.example.bff.webclient.model.DashboardResponse
import com.example.bff.webclient.service.DashboardService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val dashboardService: DashboardService,
) {
    @GetMapping("/{userId}")
    fun getDashboard(@PathVariable("userId") userId: String): DashboardResponse =
        // block() parks the MVC request thread until the reactive fan-out completes; each call is bounded by the timeout.
        requireNotNull(dashboardService.fetchDashboard(userId).block()) { "empty dashboard for $userId" }
}
