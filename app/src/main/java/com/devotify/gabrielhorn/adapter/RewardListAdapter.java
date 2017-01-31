package com.devotify.gabrielhorn.adapter;

import android.content.Context;
import android.widget.TextView;

import com.devotify.gabrielhorn.R;
import com.devotify.gabrielhorn.model.LocalUser;
import com.devotify.gabrielhorn.model.Reward;
import com.devotify.gabrielhorn.viewHelpers.BaseParseArrayAdapter;
import com.devotify.gabrielhorn.viewHelpers.ViewHolder;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;

public class RewardListAdapter extends BaseParseArrayAdapter<Reward>
{
    public RewardListAdapter(Context context, int itemViewResource)
    {
        super(context, new ParseQueryAdapter.QueryFactory()
        {
            @Override
            public ParseQuery create()
            {
                ParseQuery<Reward> query = ParseQuery.getQuery("Rewards");
                query.whereEqualTo("appCompany", LocalUser.getInstance().getParentCompany());
                query.addAscendingOrder("pointsNeeded");
                return query;
            }
        }, itemViewResource);
    }

    @Override
    protected void initView(ViewHolder holder, Reward data)
    {
        TextView pointsNeeded = (TextView) holder.getView(R.id.reward_points_text_view);
        TextView rewardName = (TextView) holder.getView(R.id.reward_name_text_view);

        pointsNeeded.setText(Integer.toString(data.getPointsNeeded()));
        rewardName.setText(data.getName());
    }
}