package com.gymplanner.plan;

import com.gymplanner.common.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plan_days")
public class PlanDay extends BaseEntity implements WorkoutPlan.Positioned {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private WorkoutPlan plan;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int position;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<PlanItem> items = new ArrayList<>();

    protected PlanDay() {
    }

    PlanDay(WorkoutPlan plan, String name, int position) {
        this.plan = plan;
        this.name = name;
        this.position = position;
    }

    public PlanItem addItem(PlanItem.Values values) {
        PlanItem item = new PlanItem(this, items.size(), values);
        items.add(item);
        return item;
    }

    public void removeItem(PlanItem item) {
        items.remove(item);
        WorkoutPlan.renumber(items);
    }

    public WorkoutPlan getPlan() {
        return plan;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int position) {
        this.position = position;
    }

    public List<PlanItem> getItems() {
        return items;
    }
}
