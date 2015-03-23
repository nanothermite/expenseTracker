package models;

import com.avaje.ebean.annotation.Sql;

import javax.persistence.Entity;

@Entity
@Sql
public class MemberUser {
    public Member member;
    public Uzer uzer;

    public Member getMember() {
        return member;
    }
    public void setMember(Member member) {
        this.member = member;
    }
    public Uzer getUzer() {
        return uzer;
    }
    public void setUzer(Uzer uzer) {
        this.uzer = uzer;
    }
}