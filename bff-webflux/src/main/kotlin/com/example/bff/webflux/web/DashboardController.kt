package com.example.bff.webflux.web

import com.example.bff.webflux.model.DashboardResponse
import com.example.bff.webflux.service.DashboardService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val dashboardService: DashboardService,
) {
    @GetMapping("/{userId}")
    fun getDashboard(@PathVariable("userId") userId: String): Mono<DashboardResponse> =
        dashboardService.fetchDashboard(userId)
}
