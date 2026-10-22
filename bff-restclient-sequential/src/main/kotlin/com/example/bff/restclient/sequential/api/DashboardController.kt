package com.example.bff.restclient.sequential.api

import com.example.bff.restclient.sequential.service.DashboardService
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
        dashboardService.buildDashboard(userId)
}
