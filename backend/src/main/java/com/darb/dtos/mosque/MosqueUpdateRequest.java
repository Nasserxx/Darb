package com.darb.dtos.mosque;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for updating a mosque")
public class MosqueUpdateRequest {

    @Size(max = 200)
    @Schema(description = "Mosque name")
    private String name;

    @Schema(description = "Full address")
    private String address;

    @Size(max = 100)
    @Schema(description = "City")
    private String city;

    @Size(max = 30)
    @Schema(description = "Contact phone")
    private String phone;

    @Size(max = 255)
    @Schema(description = "Contact email")
    private String email;

    @Schema(description = "Mosque logo URL")
    private String logoUrl;

    @Size(max = 50)
    @Schema(description = "Timezone")
    private String timezone;

    @Schema(description = "Mosque settings (JSON)")
    private String settings;
}
