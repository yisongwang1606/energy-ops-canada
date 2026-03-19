package ca.yisong.energyops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ApiWorkflowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void frontendSelectDataSourcesAreSeededAndReachable() throws Exception {
        String token = loginAsOpsLead();

        JsonNode sites = getJson(token, "/api/sites");
        JsonNode assets = getJson(token, "/api/assets");
        JsonNode alerts = getJson(token, "/api/alerts");
        JsonNode workOrders = getJson(token, "/api/work-orders");
        JsonNode maintenance = getJson(token, "/api/maintenance-records");

        assertThat(sites.isArray()).isTrue();
        assertThat(sites.size()).isGreaterThan(0);
        assertThat(assets.isArray()).isTrue();
        assertThat(assets.size()).isGreaterThan(0);
        assertThat(alerts.isArray()).isTrue();
        assertThat(alerts.size()).isGreaterThan(0);
        assertThat(workOrders.isArray()).isTrue();
        assertThat(workOrders.size()).isGreaterThan(0);
        assertThat(maintenance.isArray()).isTrue();
        assertThat(workOrders.size()).isGreaterThan(maintenance.size());
        assertThat(hasUnresolvedAlert(alerts)).isTrue();
    }

    @Test
    void actuatorEndpointsAreAvailableWithoutAuthentication() throws Exception {
        ResponseEntity<JsonNode> healthResponse = restTemplate.getForEntity(actuatorUrl("/health"), JsonNode.class);
        ResponseEntity<JsonNode> infoResponse = restTemplate.getForEntity(actuatorUrl("/info"), JsonNode.class);

        assertThat(healthResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(healthResponse.getBody()).isNotNull();
        assertThat(healthResponse.getBody().path("status").asText()).isEqualTo("UP");
        assertThat(infoResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(infoResponse.getBody()).isNotNull();
        assertThat(infoResponse.getBody().path("app").path("name").asText()).isEqualTo("Energy Ops Canada");
    }

    @Test
    void operationsEngineerCanCreateWorkOrderFromAssetWithoutAlert() throws Exception {
        String token = loginAsOpsLead();

        mockMvc.perform(post("/api/work-orders")
                        .with(csrf().asHeader())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetId": "AST-PMP-101",
                                  "title": "Field verify pump lubrication",
                                  "description": "Created from the integration test flow.",
                                  "priority": "MEDIUM",
                                  "assignedTo": "morgan.tech",
                                  "dueDate": "2026-03-20"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetId").value("AST-PMP-101"))
                .andExpect(jsonPath("$.title").value("Field verify pump lubrication"))
                .andExpect(jsonPath("$.alertId").isEmpty());
    }

    @Test
    void technicianCanExportAlertsCsv() throws Exception {
        String token = loginAsTechnician();

        mockMvc.perform(get("/api/alerts/export")
                        .header("Authorization", bearer(token))
                        .header("Accept", "text/csv"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType()).startsWith("text/csv"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).contains("alert_code,alert_type"));
    }

    private String loginAsOpsLead() throws Exception {
        return login("ops.lead", "ops123");
    }

    private String loginAsTechnician() throws Exception {
        return login("morgan.tech", "tech123");
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode getJson(String token, String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private boolean hasUnresolvedAlert(JsonNode alerts) {
        for (JsonNode alert : alerts) {
            if (!"RESOLVED".equals(alert.path("status").asText())) {
                return true;
            }
        }
        return false;
    }

    private String actuatorUrl(String path) {
        return "http://127.0.0.1:" + port + "/actuator" + path;
    }
}
