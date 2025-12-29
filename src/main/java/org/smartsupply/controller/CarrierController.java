package org.smartsupply.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smartsupply.dto.request.CarrierRequestDto;
import org.smartsupply.dto.response.CarrierResponseDto;
import org.smartsupply.dto.response.CarrierSimpleDto;
import org.smartsupply.service.CarrierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carriers")
@RequiredArgsConstructor
public class CarrierController {

    private final CarrierService carrierService;

    @PostMapping
    public ResponseEntity<CarrierResponseDto>createCarrier(@Valid @RequestBody CarrierRequestDto carrierRequestDto){

        CarrierResponseDto responseDto = carrierService.createCarrier(carrierRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping
   // @RequireAuth
    public ResponseEntity<List<CarrierResponseDto>> list() {
        return ResponseEntity.ok(carrierService.getAllCarriers());
    }

    @GetMapping("/simple")
    //@RequireAuth
    public ResponseEntity<List<CarrierSimpleDto>> listSimple() {
        return ResponseEntity.ok(carrierService.getAllSimple());
    }
    @GetMapping("/{id}")
   // @RequireAuth
    public ResponseEntity<CarrierResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(carrierService.getCarrierById(id));
    }

    @PutMapping("/{id}")
    //@RequireRole({Role.ADMIN})
    public ResponseEntity<CarrierResponseDto> update(@PathVariable Long id, @Valid @RequestBody CarrierRequestDto req) {
        return ResponseEntity.ok(carrierService.updateCarrier(id, req));
    }


    @DeleteMapping("/{id}")
   // @RequireRole({Role.ADMIN})
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carrierService.deleteCarrier(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
   // @RequireAuth
    public ResponseEntity<List<CarrierResponseDto>> search(@RequestParam String q) {
        return ResponseEntity.ok(carrierService.searchCarriers(q));
    }




}
