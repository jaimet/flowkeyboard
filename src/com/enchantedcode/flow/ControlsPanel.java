package com.enchantedcode.flow;

/**
 * Copyright 2011-2013 by Peter Eastman
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.*;

public class ControlsPanel extends View
{
  private FlowInputMethod im;
  private int repeatKey;
  private RectF leftBounds, rightBounds, upBounds, downBounds, cutBounds, copyBounds, pasteBounds, settingsBounds;
  private final Handler handler;

  public ControlsPanel(FlowInputMethod context)
  {
    super(context);
    im = context;
    handler = new Handler();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event)
  {
    float x = event.getX();
    float y = event.getY();
    if (event.getAction() == MotionEvent.ACTION_DOWN)
    {
      InputConnection ic = im.getCurrentInputConnection();
      repeatKey = -1;
      if (leftBounds.contains(x, y))
        repeatKey = KeyEvent.KEYCODE_DPAD_LEFT;
      else if (rightBounds.contains(x, y))
        repeatKey = KeyEvent.KEYCODE_DPAD_RIGHT;
      else if (upBounds.contains(x, y))
        repeatKey = KeyEvent.KEYCODE_DPAD_UP;
      else if (downBounds.contains(x, y))
        repeatKey = KeyEvent.KEYCODE_DPAD_DOWN;
      else if (settingsBounds.contains(x, y))
        im.startActivity(new Intent(im, SettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      else if (cutBounds.contains(x, y))
        ic.performContextMenuAction(android.R.id.cut);
      else if (copyBounds.contains(x, y))
        ic.performContextMenuAction(android.R.id.copy);
      else if (pasteBounds.contains(x, y))
        ic.performContextMenuAction(android.R.id.paste);
      if (repeatKey != -1) {
        im.sendDownUpKeyEvents(repeatKey);
        handler.postDelayed(new Runnable() {
          public void run()
          {
            if (repeatKey != -1)
            {
              im.sendDownUpKeyEvents(repeatKey);
              handler.postDelayed(this, im.getTouchListener().getBackspaceDelay());
            }
          }
        }, im.getTouchListener().getLongPressDelay());
      }
    }
    else if (event.getAction() == MotionEvent.ACTION_UP)
      repeatKey = -1;
    return true;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    // This is copied from KeyboardView to make this panel the same size as that one.

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    WindowManager wm = (WindowManager) im.getSystemService(Context.WINDOW_SERVICE);
    int width = getMeasuredWidth();
    int height = getMeasuredHeight();
    if (im.isFullscreenMode())
      height = Math.min(height, (int) (0.65f*wm.getDefaultDisplay().getHeight()));
    int spacing, offset;
    if (width/7 < height/5)
    {
      spacing = width/7;
      offset = (width-6*spacing)/2;
    }
    else
    {
      spacing = height/5;
      offset = (height-4*spacing)/2;
    }
    height = Math.min(height, 5*spacing+offset/8);
    setMeasuredDimension(width, height);
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    Paint paint = new Paint();
    int width = getWidth();
    int spacing = im.getKeyboardView().getKeySpacing();
    float gap = 0.07f*spacing;
    float cornerRadius = 0.2f*spacing;
    float textSize = 0.5f*spacing;
    float minx = width/2-3.5f*spacing;
    float maxx = width/2+3.5f*spacing;
    paint.setColor(Color.WHITE);
    canvas.drawRect(0, 0, width, 2*spacing+gap-cornerRadius, paint);
    canvas.drawRoundRect(new RectF(0, 0, width, 2*spacing+gap), cornerRadius, cornerRadius, paint);
    paint.setAntiAlias(true);
    paint.setTextSize(textSize);
    paint.setTextAlign(Paint.Align.CENTER);
    float x = minx+gap;
    leftBounds = new RectF(x, gap, x+spacing-2*gap, spacing-gap);
    x += spacing;
    rightBounds = new RectF(x, gap, x+spacing-2*gap, spacing-gap);
    x += spacing;
    upBounds = new RectF(x, gap, x+spacing-2*gap, spacing-gap);
    x += spacing;
    downBounds = new RectF(x, gap, x+spacing-2*gap, spacing-gap);
    x += spacing;
    settingsBounds = new RectF(x, gap, maxx-gap, spacing-gap);
    float buttonWidth = (maxx-minx)/3;
    cutBounds = new RectF(minx+gap, spacing+gap, minx+buttonWidth-gap, 2*spacing-gap);
    copyBounds = new RectF(minx+buttonWidth+gap, spacing+gap, minx+2*buttonWidth-gap, 2*spacing-gap);
    pasteBounds = new RectF(minx+2*buttonWidth+gap, spacing+gap, maxx-gap, 2*spacing-gap);
    paint.setShader(new LinearGradient(0, gap, 0, spacing-gap, new int[]{Color.rgb(210, 220, 210), Color.rgb(220, 230, 220), Color.rgb(192, 202, 192)}, new float[] {0, 0.4f, 1}, Shader.TileMode.CLAMP));
    canvas.drawRoundRect(leftBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(rightBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(upBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(downBounds, cornerRadius, cornerRadius, paint);
    paint.setShader(new LinearGradient(0, gap, 0, spacing-gap, new int[]{Color.rgb(210, 210, 220), Color.rgb(220, 220, 230), Color.rgb(192, 192, 202)}, new float[]{0, 0.4f, 1}, Shader.TileMode.CLAMP));
    canvas.drawRoundRect(settingsBounds, cornerRadius, cornerRadius, paint);
    paint.setShader(new LinearGradient(0, spacing+gap, 0, 2*spacing-gap, new int[]{Color.rgb(220, 220, 210), Color.rgb(230, 230, 220), Color.rgb(202, 202, 192)}, new float[]{0, 0.4f, 1}, Shader.TileMode.CLAMP));
    canvas.drawRoundRect(cutBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(copyBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(pasteBounds, cornerRadius, cornerRadius, paint);
    paint.setShader(null);
    paint.setColor(Color.BLACK);
    paint.setStyle(Paint.Style.STROKE);
    canvas.drawRoundRect(leftBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(rightBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(upBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(downBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(settingsBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(cutBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(copyBounds, cornerRadius, cornerRadius, paint);
    canvas.drawRoundRect(pasteBounds, cornerRadius, cornerRadius, paint);
    canvas.drawText("\u2190", leftBounds.left+0.5f*(leftBounds.right-leftBounds.left), leftBounds.top+0.6f*spacing, paint);
    canvas.drawText("\u2192", rightBounds.left+0.5f*(rightBounds.right-rightBounds.left), rightBounds.top+0.6f*spacing, paint);
    canvas.drawText("\u2191", upBounds.left+0.5f*(upBounds.right-upBounds.left), upBounds.top+0.6f*spacing, paint);
    canvas.drawText("\u2193", downBounds.left+0.5f*(downBounds.right-downBounds.left), downBounds.top+0.6f*spacing, paint);
    paint.setTextSize(0.4f*spacing);
    canvas.drawText(im.getResources().getString(R.string.settings), settingsBounds.left+0.5f*(settingsBounds.right-settingsBounds.left), settingsBounds.top+0.6f*spacing, paint);
    canvas.drawText(im.getResources().getString(R.string.cut), cutBounds.left+0.5f*(cutBounds.right-cutBounds.left), cutBounds.top+0.6f*spacing, paint);
    canvas.drawText(im.getResources().getString(R.string.copy), copyBounds.left+0.5f*(copyBounds.right-copyBounds.left), copyBounds.top+0.6f*spacing, paint);
    canvas.drawText(im.getResources().getString(R.string.paste), pasteBounds.left+0.5f*(pasteBounds.right-pasteBounds.left), pasteBounds.top+0.6f*spacing, paint);
    canvas.drawLine(cornerRadius, 2*spacing+gap+1, width-cornerRadius, 2*spacing+gap+1, paint);
  }
}
