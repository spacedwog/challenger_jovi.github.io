package io.jovi.upload.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldUploadImageAndReturnMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "smartshot.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image".getBytes()
        );

        mockMvc.perform(multipart("/api/uploads")
                        .file(file)
                        .param("userId", "7")
                        .param("mode", "Retrato")
                        .param("quality", "Alta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaId").isNumber())
                .andExpect(jsonPath("$.captureId").isNumber())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.mode").value("Retrato"))
                .andExpect(jsonPath("$.fileUrl", containsString("/api/uploads/")));
    }

    @Test
    void shouldRejectNonImageUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "plain-text".getBytes()
        );

        mockMvc.perform(multipart("/api/uploads").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A API aceita apenas arquivos de imagem."));
    }

    @Test
    void shouldServeHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/uploads/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("java-upload-api"));
    }

    @Test
    void shouldUploadAndShareFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "smartshot.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-jpeg".getBytes()
        );

        String response = mockMvc.perform(multipart("/api/uploads").file(file))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String mediaId = response.replaceAll(".*\"mediaId\":(\\d+).*", "$1");

        mockMvc.perform(multipart("/api/uploads/{mediaId}/share", mediaId)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        })
                        .param("platform", "Instagram"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("Instagram"))
                .andExpect(jsonPath("$.status").value("enviado"));

        mockMvc.perform(get("/api/uploads/{mediaId}/file", mediaId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE));
    }
}
