package com.att.tdp.issueflow.ticket.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WorkloadResponse {
    private final Long userId;
    private final String username;
    private final long openTicketCount;
}
