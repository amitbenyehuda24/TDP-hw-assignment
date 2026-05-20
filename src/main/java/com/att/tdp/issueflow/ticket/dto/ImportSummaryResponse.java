package com.att.tdp.issueflow.ticket.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ImportSummaryResponse {

    private final int created;
    private final int failed;
    private final List<String> errors;
}
