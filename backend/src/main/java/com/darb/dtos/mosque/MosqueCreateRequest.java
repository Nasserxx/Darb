package com.darb.dtos.mosque;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for creating a mosque")
public class MosqueCreateRequest {

    @NotBlank @Size(max = 200)
    @Schema(description = "Mosque name", example = "Al-Noor Mosque")
    private String name;

    @Schema(description = "Full address", example = "123 King Fahd Road, Riyadh")
    private String address;

    @Size(max = 100)
    @Schema(description = "City", example = "Riyadh")
    private String city;

    @Size(max = 30)
    @Schema(description = "Contact phone", example = "+966112345678")
    private String phone;

    @Size(max = 255)
    @Schema(description = "Contact email", example = "info@alnoor-mosque.sa")
    private String email;

    @Schema(description = "Mosque logo URL")
    private String logoUrl;

    @Size(max = 50)
    @Schema(description = "Timezone", example = "Asia/Riyadh")
    private String timezone;
}
