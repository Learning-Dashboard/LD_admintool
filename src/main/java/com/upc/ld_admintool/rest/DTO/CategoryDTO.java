package com.upc.ld_admintool.rest.DTO;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class CategoryDTO {
    private String category;
    private String patternGroup; 
    private List<Map<String, String>> interval;
}
