package com.syncflow.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionDTO {
    private Long id;
    private String module;
    private String action;
    private String description;
    private String authority;
}
