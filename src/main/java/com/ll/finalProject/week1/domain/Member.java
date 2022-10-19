package com.ll.finalProject.week1.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Member extends BaseEntity {
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Long id;

    @Column(unique = true, name = "USER_NAME")
    private String userName;

    @Column( name = "USER_PASSWORD")
    private String password;

    @Column(unique = true, name = "USER_NICKNAME")
    private String nickName;

    @Column(unique = true, name = "USER_EMAIL")
    private String email;

    @Column( name = "USER_AUTH")
    private Integer authLevel;

}