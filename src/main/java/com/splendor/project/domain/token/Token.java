package com.splendor.project.domain.token;

import com.splendor.project.domain.data.GemType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long tokenId ;


    @Enumerated(EnumType.STRING)
    private GemType gemType ;

    private int totalCount ;
}