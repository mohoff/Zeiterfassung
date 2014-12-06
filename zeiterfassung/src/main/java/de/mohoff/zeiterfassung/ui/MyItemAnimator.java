package de.mohoff.zeiterfassung.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TPPOOL01 on 29.11.2014.
 */
public class MyItemAnimator extends RecyclerView.ItemAnimator {

    List<RecyclerView.ViewHolder> viewHolders = new ArrayList<RecyclerView.ViewHolder>();

    public MyItemAnimator(){
        String test = "";
    }

    @Override
    public void runPendingAnimations() {
        if (!viewHolders.isEmpty()) {
            int animationDuration = 300;
            AnimatorSet animator;
            View target;
            for (final RecyclerView.ViewHolder viewHolder : viewHolders) {
                target = viewHolder.itemView;
                target.setPivotX(target.getMeasuredWidth() / 2);
                target.setPivotY(target.getMeasuredHeight() / 2);

                animator = new AnimatorSet();

                animator.playTogether(
                        ObjectAnimator.ofFloat(target, "translationX", -target.getMeasuredWidth(), 0.0f),
                        ObjectAnimator.ofFloat(target, "alpha", target.getAlpha(), 1.0f)
                );

                animator.setTarget(target);
                animator.setDuration(animationDuration);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.setStartDelay((animationDuration * viewHolder.getPosition()) / 10);
                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {}
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        viewHolders.remove(viewHolder);
                    }
                    @Override
                    public void onAnimationCancel(Animator animation) {}
                    @Override
                    public void onAnimationRepeat(Animator animation) {}
                });
                animator.start();
            }
        }
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        return false;
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        holder.itemView.setAlpha(0.0f);
        viewHolders.add(holder);
        return true;
        //return false;
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        return false;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) {
        return false;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {

    }

    @Override
    public void endAnimations() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
