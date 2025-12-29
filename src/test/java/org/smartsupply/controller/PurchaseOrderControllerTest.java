package org.smartsupply.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smartsupply.dto.request.POLineRequestDto;
import org.smartsupply.dto.request.PurchaseOrderRequestDto;
import org.smartsupply.dto.response.PurchaseOrderResponseDto;
import org.smartsupply.service.PurchaseOrderService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(MockitoExtension.class)
class PurchaseOrderControllerTest {

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @InjectMocks
    private PurchaseOrderController controller;

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    @BeforeEach
    void prep() {
        mapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // Helper method pour générer un DTO valide
    private PurchaseOrderRequestDto buildValidPurchaseOrderRequest() {
        PurchaseOrderRequestDto dto = new PurchaseOrderRequestDto();
        dto.setSupplierId(1L); // Obligatoire
        // Ajouter d'autres champs obligatoires si nécessaires
        return dto;
    }

    private POLineRequestDto buildValidPOLineRequest() {
        POLineRequestDto line = new POLineRequestDto();
        line.setProductId(1L); // Obligatoire
        line.setQty(10);
        line.setPrice(BigDecimal.valueOf(100.00));
        return line;
    }

    @Test
    void create_list_get_addLine_approve_markReceived_delete() throws Exception {
        // --- CREATE ---
        PurchaseOrderRequestDto req = buildValidPurchaseOrderRequest();
        PurchaseOrderResponseDto dto = new PurchaseOrderResponseDto();
        dto.setId(1L);

        when(purchaseOrderService.createPurchaseOrder(any(PurchaseOrderRequestDto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        // --- GET ALL ---
        when(purchaseOrderService.getAllPurchaseOrders()).thenReturn(List.of(dto));
        mockMvc.perform(get("/api/purchase-orders"))
                .andExpect(status().isOk());

        // --- GET BY ID ---
        when(purchaseOrderService.getPurchaseOrderById(2L)).thenReturn(dto);
        mockMvc.perform(get("/api/purchase-orders/2"))
                .andExpect(status().isOk());

        // --- ADD LINE ---
        POLineRequestDto line = buildValidPOLineRequest();
        when(purchaseOrderService.addLineToPurchaseOrder(eq(2L), any(POLineRequestDto.class))).thenReturn(dto);
        mockMvc.perform(post("/api/purchase-orders/2/lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(line)))
                .andExpect(status().isOk());

        // --- APPROVE ---
        doNothing().when(purchaseOrderService).approvePurchaseOrder(3L);
        mockMvc.perform(put("/api/purchase-orders/3/approve"))
                .andExpect(status().isOk());

        // --- MARK RECEIVED ---
        doNothing().when(purchaseOrderService).markPurchaseOrderAsReceived(4L, 5L);
        mockMvc.perform(put("/api/purchase-orders/4/mark-received")
                        .param("warehouseId", "5"))
                .andExpect(status().isOk());

        // --- DELETE ---
        doNothing().when(purchaseOrderService).deletePurchaseOrder(6L);
        mockMvc.perform(delete("/api/purchase-orders/6"))
                .andExpect(status().isNoContent());

        // --- VERIFY ---
        verify(purchaseOrderService).createPurchaseOrder(any(PurchaseOrderRequestDto.class));
        verify(purchaseOrderService).getAllPurchaseOrders();
        verify(purchaseOrderService).getPurchaseOrderById(2L);
        verify(purchaseOrderService).addLineToPurchaseOrder(eq(2L), any(POLineRequestDto.class));
        verify(purchaseOrderService).approvePurchaseOrder(3L);
        verify(purchaseOrderService).markPurchaseOrderAsReceived(4L, 5L);
        verify(purchaseOrderService).deletePurchaseOrder(6L);
    }
}
