package com.netflix.contentservice.mapper;
/**
 * Global base contract for all MapStruct mappers.
 * Every mapper in this project implements this.
 *
 * E = Entity
 * Q = Request DTO  (inbound)
 * S = Response DTO (outbound)
 */
public interface BaseMapper<E, Q, S> {

    E toEntity(Q request);

    S toResponse(E entity);
}
