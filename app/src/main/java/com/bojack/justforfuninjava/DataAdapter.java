package com.bojack.justforfuninjava;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;


public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {

    private ConcurrentHashMap<Integer, DataBean> dataSet = new ConcurrentHashMap<>();
    Random random = new Random(10000);
    QueryThread queryThread = new QueryThread();
    BlockingDeque<DataBean> blockingDeque = new LinkedBlockingDeque<>();
    Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if(msg.what == MES_DATA_CHANGED) {
                notifyDataSetChanged();
            }
        }
    };
    private static final int MES_DATA_CHANGED = 1;

    private Set<Integer> randomPool = new HashSet<>();

    DataAdapter(){
        randomPool.add(-1);
        for (int i = 0; i < 5000; i++) {
            int randomValue = -1;
            while (randomPool.contains(randomValue)){
                randomValue = random.nextInt(20000);
            }
            randomPool.add(randomValue);
            dataSet.put(i, new DataBean(String.valueOf(randomValue), null));
        }
        queryThread.start();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.e("bojack", "databean status " + dataSet.get(position).hashCode());
        Log.e("bojack", "databean status " + dataSet.get(position).number + " " + dataSet.get(position).getEmergency());

        String data1 = Objects.requireNonNull(dataSet.get(position)).getNumber();
        holder.textView1.setText(data1);
        Boolean isEmergency = dataSet.get(position).getEmergency();
        if(isEmergency != null){
            holder.textView2.setText( isEmergency ? "紧急号码" : "非紧急号码");
        }else {
            holder.textView2.setText("未知");
            if(randomPool.contains(Integer.parseInt(dataSet.get(position).number))) {
                blockingDeque.offer(dataSet.get(position));
                randomPool.remove(Integer.parseInt(dataSet.get(position).number));
            }
        }
    }

    private int count;
    private boolean methodMayBlock(String obj){
        count = count + 1;
        Log.e("bojack", "methodMayBlock() invoked times: " + count);
        long randomSleepTime = random.nextInt(300);
        SystemClock.sleep(randomSleepTime);
        long time = System.currentTimeMillis();
        if(time % 2 == 0){
            return true;
        }else {
            return false;
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        TextView textView1;
        TextView textView2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView1 = itemView.findViewById(R.id.tv_data_1);
            textView2 = itemView.findViewById(R.id.tv_data_2);
        }
    }

    class DataBean{
        volatile String number;
        volatile Boolean isEmergency;
        DataBean(String number, Boolean isEmergency){
            this.number = number;
            this.isEmergency = isEmergency;
        }

        public String getNumber() {
            return number;
        }

        public Boolean getEmergency() {
            return isEmergency;
        }

        public void setEmergency(Boolean emergency) {
            isEmergency = emergency;
        }
    }

    class QueryThread extends Thread{
        @Override
        public void run() {
            super.run();
            while (true){
                DataBean dataNeedToBeParse;
                try {
                    dataNeedToBeParse = blockingDeque.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(dataNeedToBeParse != null){
                    Boolean result = methodMayBlock(dataNeedToBeParse.getNumber());
                    dataNeedToBeParse.setEmergency(result);
                    Log.e("bojack", "databean changed " + dataNeedToBeParse.hashCode());
                    Log.e("bojack", "databean changed " + dataNeedToBeParse.number + " " + dataNeedToBeParse.getEmergency());
                    mainHandler.removeMessages(MES_DATA_CHANGED);
                    mainHandler.sendEmptyMessage(MES_DATA_CHANGED);
                }
            }
        }
    }
}
