package com.upc.ld_admintool.rest.DTO;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FactorDTO {
    private String id;
    private String externalId;
    private String name;
    private String description;
    private String categoryName;
}