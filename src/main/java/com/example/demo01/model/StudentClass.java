package com.example.demo01.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_class")
public class StudentClass {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long classid;

    @Column(nullable = false)
    private String classname;

    @Column(nullable = false)
    private String classcode;

    @Column(nullable = false)
    private int classnum;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 构造函数
    public StudentClass() {}

    // Getter/Setter
    public Long getClassid() { return classid; }
    public void setClassid(Long classid) { this.classid = classid; }

    public String getClassname() { return classname; }
    public void setClassname(String classname) { this.classname = classname; }

    public String getClasscode() { return classcode; }
    public void setClasscode(String classcode) { this.classcode = classcode; }

    public int getClassnum() { return classnum; }
    public void setClassnum(int classnum) { this.classnum = classnum; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // toString 方法
    @Override
    public String toString() {
        return "StudentClass{" +
                "classid=" + classid +
                ", classname='" + classname + '\'' +
                ", classcode='" + classcode + '\'' +
                ", classnum=" + classnum +
                '}';
    }
}
