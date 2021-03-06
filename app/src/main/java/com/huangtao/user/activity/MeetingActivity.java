package com.huangtao.user.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.huangtao.dialog.QRCodeDialog;
import com.huangtao.user.R;
import com.huangtao.user.common.Constants;
import com.huangtao.user.common.MyActivity;
import com.huangtao.user.helper.CommonUtils;
import com.huangtao.user.model.Meeting;
import com.huangtao.user.model.User;
import com.huangtao.user.network.FileManagement;
import com.huangtao.user.network.Network;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import java.util.List;

import butterknife.BindView;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MeetingActivity extends MyActivity {

    @BindView(R.id.time)
    TextView time;
    @BindView(R.id.date)
    TextView date;
    @BindView(R.id.share)
    ImageView share;
    @BindView(R.id.location)
    TextView location;
    @BindView(R.id.heading)
    TextView heading;
    @BindView(R.id.status)
    TextView status;

    @BindView(R.id.content)
    TextView content;

    @BindView(R.id.host_head)
    ImageView hostHead;
    @BindView(R.id.host_name)
    TextView hostName;

    @BindView(R.id.participant_number)
    TextView participantNumber;
    @BindView(R.id.participant_container)
    GridLayout participantContainer;

    @BindView(R.id.sign)
    TextView sign;
    @BindView(R.id.type)
    TextView type;

    @BindView(R.id.number)
    TextView number;

    @BindView(R.id.enter)
    Button enter;

    Meeting meeting;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_meeting;
    }

    @Override
    protected int getTitleBarId() {
        return R.id.title;
    }

    @Override
    protected void initView() {
        getStatusBarConfig().statusBarDarkFont(false).init();
    }

    @Override
    protected void initData() {
        String id;
        if ((id = getIntent().getStringExtra("id")) == null) {
            finish();
        } else {
            showProgressBar();
            Network.getInstance().queryMeetingById(id).enqueue(new Callback<Meeting>() {
                @Override
                public void onResponse(Call<Meeting> call, Response<Meeting> response) {
                    if (response.body() != null) {
                        meeting = response.body();
                        init();
                    } else {
                        toast("返回值为空");
                    }
                }

                @Override
                public void onFailure(Call<Meeting> call, Throwable t) {
                    toast("网络请求失败");
                    t.printStackTrace();
                    hideProgressBar();
                }
            });
        }
    }

    /**
     * 传入meeting对象 or id后，更新ui
     */
    private void init() {
        time.setText(CommonUtils.getFormatTime(meeting.getStartTime(), meeting.getEndTime()));

        String month = meeting.getDate().split("-")[1];
        final String day = meeting.getDate().split("-")[2];
        date.setText(month + "/" + day + "\n" + CommonUtils.dateToWeek(meeting.getDate()));

        location.setText(meeting.getLocation());

        heading.setText(!meeting.getHeading().isEmpty() ? meeting.getHeading() : "无主题");

        String statusStr = null;
        switch (meeting.getStatus()){
            case Pending:
                statusStr = "未开始";
                break;
            case Running:
                statusStr = "进行中";
                enter.setEnabled(false);
                break;
            case Stopped:
                statusStr = "已结束";
                enter.setEnabled(false);
                break;
            case Cancelled:
                statusStr = "已取消";
                enter.setEnabled(false);
                break;
        }
        status.setText(statusStr);

        content.setText(!meeting.getDescription().isEmpty() ? meeting.getDescription() : "无内容");

        initParticipant();

        // 是否需要签到
        if(!meeting.isNeedSignIn()){
            sign.setText("否");
        }

        // 会议类型
        String typeStr = null;
        switch (meeting.getType()){
            case COMMON:
                typeStr = "普通";
                break;
            case URGENCY:
                typeStr = "紧急";
                type.setTextColor(getColor(R.color.douban_red_80_percent));
                break;
        }
        type.setText(typeStr);

        // 按钮
        if(meeting.getAttendants().keySet().contains(Constants.uid)){
            enter.setText("退出会议");
            enter.setBackground(getDrawable(R.drawable.selector_meeting_exit_button));
            enter.setTextColor(getColor(R.color.douban_red_80_percent));
            enter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 退出会议
                    exit();
                }
            });
        } else {
            enter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 加入会议
                    join();
                }
            });
        }

        // 会议代码
        number.setText(meeting.getAttendantNum());

        // 主持人
        Network.getInstance().queryUserById(meeting.getHostId()).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if(response.body() != null) {
                    User host = response.body();
                    hostName.setText(host.getName());

                    Observable.just(FileManagement.getUserHead(MeetingActivity.this, host))
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<String>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onNext(String s) {
                                    if(!s.isEmpty()){
                                        hostHead.setImageBitmap(CommonUtils.getLoacalBitmap(s));
                                    }
                                }

                                @Override
                                public void onError(Throwable e) {

                                }

                                @Override
                                public void onComplete() {

                                }
                            });

                } else {
                    toast("返回值为空");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                toast("网络请求失败");
                t.printStackTrace();
            }
        });


        // 二维码
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QRCodeDialog.Builder builder = new QRCodeDialog.Builder(MeetingActivity.this);
                builder.setMessage(meeting.getAttendantNum());
                builder.setQrcode(CodeUtils.createImage(meeting.getAttendantNum(), 400, 400, null));
//                builder.setWidth((int) (DimensUtils.getScreenWidth(MeetingActivity.this) * 0.7));
//                builder.setHeight((int) (DimensUtils.getScreenHeight(MeetingActivity.this) * 0.5));
                builder.create().show();
            }
        });





        hideProgressBar();

    }

    private void initParticipant() {
        // 参会者
        participantNumber.setText("参会者: \n(" + meeting.getAttendants().size() + ")");
        Network.getInstance().queryAttendantsFromMeeting(meeting.getId()).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                participantContainer.removeAllViews();

                List<User> users = response.body();
                for(User user : users) {
                    addParticipantView(user);
                }

                View v = LayoutInflater.from(MeetingActivity.this).inflate(R.layout.item_meeting_user, null);
                TextView attendantName = v.findViewById(R.id.name);
                ImageView attendantHead = v.findViewById(R.id.head);
                attendantName.setText("");
                attendantHead.setImageResource(R.mipmap.ic_add_gray);
                participantContainer.addView(v);
                // 添加参会者
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MeetingActivity.this, AddressBookActivity.class);
                        intent.putExtra("meeting", meeting);
                        startActivityForResult(intent, 100);
                    }
                });
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {

            }
        });
    }

    private void join() {
        showDialog("确定要加入该会议吗？", "加入", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Network.getInstance().joinMeeting(meeting.getAttendantNum(), Constants.uid).enqueue(new Callback<Meeting>() {
                    @Override
                    public void onResponse(Call<Meeting> call, Response<Meeting> response) {
                        toast("加入成功");

                        meeting = response.body();
                        participantNumber.setText("参会者: \n(" + meeting.getAttendants().size() + ")");
                        View v = participantContainer.getChildAt(participantContainer.getChildCount() - 1);
                        participantContainer.removeView(v);
                        addParticipantView(Constants.user);
                        participantContainer.addView(v);

                        enter.setText("退出会议");
                        enter.setBackground(getDrawable(R.drawable.selector_meeting_exit_button));
                        enter.setTextColor(getColor(R.color.douban_red_80_percent));
                        enter.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                exit();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<Meeting> call, Throwable t) {
                        toast("加入失败");
                    }
                });
            }
        }, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    private void exit() {
        showDialog("确定要退出该会议吗？", "退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Network.getInstance(ScalarsConverterFactory.create()).exitMeeting(meeting.getId(), Constants.uid).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        showDialog("退出会议成功", "确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        toast("退出会议失败");
                        t.printStackTrace();
                    }
                });
            }
        }, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    private void addParticipantView (User user) {
        if(user == null) {
            return;
        }

        View v = LayoutInflater.from(MeetingActivity.this).inflate(R.layout.item_meeting_user, null);
        TextView attendantName = v.findViewById(R.id.name);
        final ImageView attendantHead = v.findViewById(R.id.head);

        attendantName.setText(user.getName());
        Observable.just(FileManagement.getUserHead(MeetingActivity.this, user))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        if(!s.isEmpty()){
                            attendantHead.setImageBitmap(CommonUtils.getLoacalBitmap(s));
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
        participantContainer.addView(v);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 100 && resultCode == RESULT_OK){
            meeting = (Meeting) data.getSerializableExtra("meeting");
            initParticipant();
        }

    }
}
