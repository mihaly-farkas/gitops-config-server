package io.github.mihaly_farkas.gitops_config_server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
abstract class MockMvcTest {

  @Autowired MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;
}
