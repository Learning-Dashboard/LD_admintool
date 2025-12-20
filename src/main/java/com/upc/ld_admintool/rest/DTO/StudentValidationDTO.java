package com.upc.ld_admintool.rest.DTO;

import lombok.Data;

@Data
public class StudentValidationDTO {
    private Long projectId;
    private String githubUrl;
    private String taigaUrl;
    private String githubToken;
    private StudentDTO student;
}
