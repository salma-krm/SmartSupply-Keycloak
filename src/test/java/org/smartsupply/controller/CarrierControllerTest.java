package org.smartsupply.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smartsupply.dto.request.CarrierRequestDto;
import org.smartsupply.dto.response.CarrierResponseDto;
import org.smartsupply.dto.response.CarrierSimpleDto;
import org.smartsupply.service.CarrierService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CarrierControllerTest {

    @Mock
    private CarrierService carrierService;

    @InjectMocks
    private CarrierController controller;

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    @BeforeEach
    void init() {
        mapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private CarrierRequestDto validRequest() {
        CarrierRequestDto req = new CarrierRequestDto();
        req.setName("DHL");
        req.setPhone("+33123456789"); // required
        req.setEmail("contact@dhl.example"); // required

        return req;
    }

    @Test
    void createCarrier_returnsCreated() throws Exception {
        CarrierRequestDto req = validRequest();

        CarrierResponseDto resp = new CarrierResponseDto();
        resp.setId(10L);
        resp.setName("DHL");
        when(carrierService.createCarrier(any(CarrierRequestDto.class))).thenReturn(resp);

        mockMvc.perform(post("/api/carriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("DHL"));

        verify(carrierService).createCarrier(any(CarrierRequestDto.class));
    }

    @Test
    void list_and_listSimple_and_getById_and_search() throws Exception {
        CarrierResponseDto c1 = new CarrierResponseDto(); c1.setId(1L);
        CarrierResponseDto c2 = new CarrierResponseDto(); c2.setId(2L);
        when(carrierService.getAllCarriers()).thenReturn(Arrays.asList(c1, c2));

        CarrierSimpleDto s1 = new CarrierSimpleDto(); s1.setId(1L);
        CarrierSimpleDto s2 = new CarrierSimpleDto(); s2.setId(2L);
        when(carrierService.getAllSimple()).thenReturn(Arrays.asList(s1, s2));

        when(carrierService.getCarrierById(5L)).thenReturn(c1);
        when(carrierService.searchCarriers("q")).thenReturn(List.of(c2));

        mockMvc.perform(get("/api/carriers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        mockMvc.perform(get("/api/carriers/simple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].id").value(2));

        mockMvc.perform(get("/api/carriers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        mockMvc.perform(get("/api/carriers/search").param("q", "q"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));

        verify(carrierService).getAllCarriers();
        verify(carrierService).getAllSimple();
        verify(carrierService).getCarrierById(5L);
        verify(carrierService).searchCarriers("q");
    }

    @Test
    void update_and_delete_endpoints_callService() throws Exception {
        CarrierRequestDto req = validRequest();
        req.setName("NewCo");

        CarrierResponseDto out = new CarrierResponseDto(); out.setId(7L);
        when(carrierService.updateCarrier(eq(7L), any(CarrierRequestDto.class))).thenReturn(out);
        doNothing().when(carrierService).deleteCarrier(8L);

        mockMvc.perform(put("/api/carriers/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        mockMvc.perform(delete("/api/carriers/8"))
                .andExpect(status().isNoContent());

        verify(carrierService).updateCarrier(eq(7L), any(CarrierRequestDto.class));
        verify(carrierService).deleteCarrier(8L);
    }
}