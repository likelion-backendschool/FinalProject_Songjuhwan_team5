package com.ll.finalProject.week1.service;

import com.ll.finalProject.week1.domain.Member;
import com.ll.finalProject.week1.dto.MemberDto;
import com.ll.finalProject.week1.dto.MemberModifyDto;
import com.ll.finalProject.week1.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public void create(MemberDto memberDto) {
        Member member = new Member();
        member.setUserName(memberDto.getUserName());
        member.setNickName(memberDto.getNickName());
        member.setPassword(passwordEncoder.encode(memberDto.getPassword()));
        member.setEmail(memberDto.getEmail());
        member.setAuthLevel(3);
        memberRepository.save(member);
    }

    public void update(MemberModifyDto memberModifyDto, Member member) {
        member.setNickName(memberModifyDto.getNickName());
        member.setEmail(memberModifyDto.getEmail());
        memberRepository.save(member);
    }

    public Member findByUserName(String username) {
        return memberRepository.findByUserName(username).orElseThrow(null);
    }

    public MemberModifyDto saveNewMemberDto(Member member, MemberModifyDto memberModifyDto) {
        memberModifyDto.setEmail(member.getEmail());
        memberModifyDto.setNickName(member.getNickName());

        return memberModifyDto;
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public String findUserName(String email) {
        List<Member> memberList = memberRepository.findAll();
        for(Member member : memberList){
            if(member.getEmail().equals(email)){
                return member.getUserName();
            }
        }
        String wrong = "해당 e-mail로 등록된 ID가 존재하지 않습니다.";
        return wrong;
    }
}