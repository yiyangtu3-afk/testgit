package com.campuslink.entity;

import com.campuslink.entity.DemoEntities.PostEntity;

public record PostLikeResult(PostEntity post, boolean liked) {
}
