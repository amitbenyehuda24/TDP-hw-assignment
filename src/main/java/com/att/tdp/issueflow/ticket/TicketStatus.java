package com.att.tdp.issueflow.ticket;

import java.util.Set;

public enum TicketStatus {

    TODO {
        @Override
        public Set<TicketStatus> validTransitions() { return Set.of(IN_PROGRESS); }
    },
    IN_PROGRESS {
        @Override
        public Set<TicketStatus> validTransitions() { return Set.of(IN_REVIEW); }
    },
    IN_REVIEW {
        @Override
        public Set<TicketStatus> validTransitions() { return Set.of(DONE); }
    },
    DONE {
        @Override
        public Set<TicketStatus> validTransitions() { return Set.of(); }
    };

    public abstract Set<TicketStatus> validTransitions();

    public boolean canTransitionTo(TicketStatus next) {
        return validTransitions().contains(next);
    }
}
