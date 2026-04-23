package com.example.demo01.model;

import jakarta.persistence.*;

@Entity
@Table(name = "question_items")
public class question_items {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true,length = 2000)
    private String question_body;

    @Column(nullable = true)
    private int question_type;
    //0 选择题   1主观题

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion_body() {
        return question_body;
    }

    public void setQuestion_body(String question_body) {
        this.question_body = question_body;
    }

    public int getQuestion_type() {
        return question_type;
    }

    public void setQuestion_type(int question_type) {
        this.question_type = question_type;
    }

    public String getQuestion_answer() {
        return question_answer;
    }

    public void setQuestion_answer(String question_answer) {
        this.question_answer = question_answer;
    }

    public String getChoosing_answer() {
        return choosing_answer;
    }

    public void setChoosing_answer(String choosing_answer) {
        this.choosing_answer = choosing_answer;
    }

    public Long getUserid() {
        return userid;
    }

    public void setUserid(Long userid) {
        this.userid = userid;
    }

    @Column(nullable = true,length = 2000)
    private String question_answer;

    @Column(nullable = true,length = 2000)
    private String choosing_answer;
    //选择题的选项列

    @Column(nullable = true)
    private Long userid;
    // 生成题目的用户 ID


    @Override
    public String toString() {
        return "question_items{" +
                "id=" + id +
                ", question_body='" + question_body + '\'' +
                ", question_type=" + question_type +
                ", question_answer='" + question_answer + '\'' +
                ", choosing_answer='" + choosing_answer + '\'' +
                ", userid=" + userid +
                '}';
    }
}
