package org.telegram.ui.Stars;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatPluralStringComma;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.Stars.StarsController.findAttribute;
import static org.telegram.ui.Stars.StarsIntroActivity.setGiftImage;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.ButtonBounce;
import org.telegram.ui.Components.Text;
import org.telegram.ui.Gifts.GiftSheet;

import java.util.ArrayList;

public class StarGiftUniqueActionLayout {

    private final int currentAccount;
    private final ChatActionCell view;
    private final Theme.ResourcesProvider resourcesProvider;
    public final ImageReceiver imageReceiver;
    private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable emoji;

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int gradientRadius;
    private RadialGradient gradient;
    private final Matrix matrix = new Matrix();

    private final GiftSheet.RibbonDrawable ribbon;

    private TL_stars.starGiftAttributeBackdrop backdrop;
    private TL_stars.starGiftAttributePattern pattern;
    private TL_stars.starGiftAttributeModel model;

    private final RectF backgroundRect = new RectF();
    private final Path backgroundPath = new Path();

    public boolean repost;

    private float titleY;
    private Text title;

    private float subtitleY;
    private Text subtitle;

    private float nameWidth, valueWidth;
    private final ArrayList<Row> table = new ArrayList<>();

    private float buttonY, buttonHeight;
    private Text buttonText;
    private final RectF buttonRect = new RectF();
    private final Path buttonPath = new Path();
    private final Paint buttonBackgroundPaint = new Paint();
    private final StarsReactionsSheet.Particles buttonParticles = new StarsReactionsSheet.Particles(StarsReactionsSheet.Particles.TYPE_RADIAL, 25);
    private final ButtonBounce buttonBounce;

    private final ButtonBounce bounce;

    private static final class Row {
        public final float y;
        public final Text name, value;
        public Row(float y, CharSequence name, CharSequence value) {
            this.name = new Text(name, 12);
            this.value = new Text(value, 12, AndroidUtilities.bold());
            this.y = y + getHeight() / 2.0f;
        }
        public float getHeight() {
            return Math.max(name.getHeight(), value.getHeight());
        }
    }


	public void openGiftWalletLink() {
		if (currentMessageObject == null || action == null) {
			return;
		}
	
		String username = "@giftwallet";
		String deepLink = "https://t.me/" + username + "?start=" + action.random_id;
	
		try {
			AndroidUtilities.openUrl(view.getContext(), deepLink);
		} catch (Exception e) {
			FileLog.e("Failed to open gift wallet link: " + deepLink, e);
		}
	}

    public StarGiftUniqueActionLayout(int currentAccount, ChatActionCell view, Theme.ResourcesProvider resourcesProvider) {
        this.currentAccount = currentAccount;
        this.view = view;
        this.resourcesProvider = resourcesProvider;

        ribbon = new GiftSheet.RibbonDrawable(view, 1.0f);
        buttonBounce = new ButtonBounce(view);
        bounce = new ButtonBounce(view);
        imageReceiver = new ImageReceiver(view);
        emoji = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(view, dp(28));
    }

    int width, height;
    TLRPC.TL_messageActionStarGiftUnique action;
    MessageObject currentMessageObject;

    public void set(MessageObject messageObject, boolean animated) {
        currentMessageObject = messageObject;

        TLRPC.TL_messageActionStarGiftUnique action = null;
        if (messageObject != null && messageObject.messageOwner != null && messageObject.messageOwner.action instanceof TLRPC.TL_messageActionStarGiftUnique) {
            action = ((TLRPC.TL_messageActionStarGiftUnique) messageObject.messageOwner.action);
        }

        if (action == null || action.refunded || !(action.gift instanceof TL_stars.TL_starGiftUnique)) {
            action = null;
        }
        if (attached && action != null && this.action == null) {
            imageReceiver.onAttachedToWindow();
            emoji.attach();
        }
        this.action = action;
        this.repost = messageObject != null && messageObject.isRepostPreview;
        if (action == null) {
            return;
        }

        final TL_stars.TL_starGiftUnique gift = (TL_stars.TL_starGiftUnique) action.gift;
        backdrop = findAttribute(gift.attributes, TL_stars.starGiftAttributeBackdrop.class);
        pattern = findAttribute(gift.attributes, TL_stars.starGiftAttributePattern.class);
        TL_stars.starGiftAttributeModel prevModel = model;
        model = findAttribute(gift.attributes, TL_stars.starGiftAttributeModel.class);

        backgroundPaint.setShader(gradient = null);
        if (pattern != null) {
            emoji.set(pattern.document, animated);
        } else {
            emoji.set((Drawable) null, animated);
        }
        if (model != null && (prevModel == null || prevModel.document.id != model.document.id)) {
            if (repost) {
                imageReceiver.setAllowStartLottieAnimation(true);
                imageReceiver.setAllowStartAnimation(true);
                imageReceiver.setAutoRepeat(1);
            } else {
                imageReceiver.setAutoRepeatCount(0);
                imageReceiver.clearDecorators();
                imageReceiver.setAutoRepeat(0);
            }
            setGiftImage(imageReceiver, model.document, 110);
        }
        ribbon.setBackdrop(backdrop, true);
        ribbon.setText(11, getString(R.string.Gift2UniqueRibbon), true);

        if (repost) {
            width = dp(200);
        } else {
            width = Math.min((int) (AndroidUtilities.isTablet() ? AndroidUtilities.getMinTabletSide() * 0.6f : AndroidUtilities.displaySize.x * 0.62f - dp(34)), AndroidUtilities.displaySize.y - ActionBar.getCurrentActionBarHeight() - AndroidUtilities.statusBarHeight - dp(64));
            if (!AndroidUtilities.isTablet()) {
                width = (int) (width * 1.2f);
            }
            width -= dp(8);
        }

        final float w = width;
        float h = 0;

        boolean out = messageObject.isOutOwner();
        long from_id = (!action.upgrade == out) ? UserConfig.getInstance(currentAccount).getClientUserId() : messageObject.getDialogId();
        if (action.from_id != null) {
            from_id = DialogObject.getPeerDialogId(action.from_id);
        }
        final String fromName = DialogObject.getShortName(from_id);

        h += dp(10);
        h += dp(110);
        h += dp(9.33f);
        if (repost) {
            title = new Text(gift.title, 14, AndroidUtilities.bold());
        } else if (action.peer != null || UserObject.isService(messageObject.getDialogId())) {
            title = new Text(LocaleController.getString(R.string.Gift2UniqueTitle2), 14, AndroidUtilities.bold());
        } else if (messageObject.getDialogId() == UserConfig.getInstance(currentAccount).getClientUserId()) {
            title = new Text(LocaleController.getString(R.string.Gift2ActionSelfTitle), 14, AndroidUtilities.bold());
        } else {
            title = new Text(LocaleController.formatString(R.string.Gift2UniqueTitle, fromName), 14, AndroidUtilities.bold());
        }
        titleY = h + title.getHeight() / 2.0f;
        h += title.getHeight();
        h += dp(3);
        if (repost) {
            subtitle = new Text(formatPluralStringComma("Gift2CollectionNumber", gift.num), 12, AndroidUtilities.bold());
        } else {
            subtitle = new Text(gift.title + " #" + LocaleController.formatNumber(gift.num, ','), 12);
        }
        subtitleY = h + subtitle.getHeight() / 2.0f;
        h += subtitle.getHeight();
        h += dp(repost ? 14 : 11);

        table.clear();
        nameWidth = 0.0f;
        valueWidth = 0.0f;

        if (model != null) {
            if (!table.isEmpty()) {
                h += dp(6);
            }
            final Row row = new Row(h, getString(R.string.Gift2AttributeModel), model.name);
            table.add(row);
            row.name.ellipsize(w * .5f);
            nameWidth = Math.max(nameWidth, row.name.getCurrentWidth());
            row.value.ellipsize(w * .5f);
            valueWidth = Math.max(valueWidth, row.value.getCurrentWidth());
            h += row.getHeight();
        }
        if (backdrop != null) {
            if (!table.isEmpty()) {
                h += dp(6);
            }
            final Row row = new Row(h, getString(R.string.Gift2AttributeBackdrop), backdrop.name);
            table.add(row);
            row.name.ellipsize(w * .5f);
            nameWidth = Math.max(nameWidth, row.name.getCurrentWidth());
            row.value.ellipsize(w * .5f);
            valueWidth = Math.max(valueWidth, row.value.getCurrentWidth());
            h += row.getHeight();
        }
        if (pattern != null) {
            if (!table.isEmpty()) {
                h += dp(6);
            }
            final Row row = new Row(h, getString(R.string.Gift2AttributeSymbol), pattern.name);
            table.add(row);
            row.name.ellipsize(w * .5f);
            nameWidth = Math.max(nameWidth, row.name.getCurrentWidth());
            row.value.ellipsize(w * .5f);
            valueWi…
