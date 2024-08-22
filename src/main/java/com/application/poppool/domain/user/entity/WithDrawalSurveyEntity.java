package com.application.poppool.domain.user.entity;

import com.application.poppool.domain.user.enums.WithDrawlSurvey;
import com.application.poppool.global.audit.BaseEntity;
import com.application.poppool.global.converter.WithDrawlSurveyConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_withdrawl_survey")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class WithDrawalSurveyEntity extends BaseEntity {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SURVEY")
    @Convert(converter = WithDrawlSurveyConverter.class)
    private WithDrawlSurvey survey;

    @Column(name = "CNT")
    @Builder.Default
    private long count = 0;

    public void incrementCount() {
        this.count++;
    }
}
