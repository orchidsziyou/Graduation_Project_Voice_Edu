package com.example.demo01.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_members")  // 表名也建议统一为下划线风格
public class ClassMembers {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Long userid;

    @Column(nullable = false)
    private Long classid;  // 关联到 StudentClass

    @Column(nullable = false)
    private int userrole;
    // 1 为学生，5 为老师

    private LocalDateTime joinAt;  // 加入时间

    // 构造函数
    public ClassMembers() {}

    // Getter/Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserid() { return userid; }
    public void setUserid(Long userid) { this.userid = userid; }

    public Long getClassid() { return classid; }
    public void setClassid(Long classid) { this.classid = classid; }

    public int getUserrole() { return userrole; }
    public void setUserrole(int userrole) { this.userrole = userrole; }

    public LocalDateTime getJoinAt() { return joinAt; }
    public void setJoinAt(LocalDateTime joinAt) { this.joinAt = joinAt; }

    // toString 方法
    @Override
    public String toString() {
        return "ClassMembers{" +
                "id=" + id +
                ", userid=" + userid +
                ", classid=" + classid +
                ", userrole=" + userrole +
                '}';
    }
}
