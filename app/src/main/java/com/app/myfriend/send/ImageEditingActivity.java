package com.app.myfriend.send;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.app.myfriend.R;
import com.app.myfriend.menu.MenuActivity;
import com.app.myfriend.story.AddStoryActivity;

import java.io.File;

import ly.img.android.pesdk.PhotoEditorSettingsList;
import ly.img.android.pesdk.assets.filter.basic.FilterPackBasic;
import ly.img.android.pesdk.assets.font.basic.FontPackBasic;
import ly.img.android.pesdk.assets.frame.basic.FramePackBasic;
import ly.img.android.pesdk.assets.overlay.basic.OverlayPackBasic;
import ly.img.android.pesdk.assets.sticker.emoticons.StickerPackEmoticons;
import ly.img.android.pesdk.assets.sticker.shapes.StickerPackShapes;
import ly.img.android.pesdk.backend.model.EditorSDKResult;
import ly.img.android.pesdk.backend.model.state.LoadSettings;
import ly.img.android.pesdk.backend.model.state.PhotoEditorSaveSettings;
import ly.img.android.pesdk.backend.model.state.manager.SettingsList;
import ly.img.android.pesdk.ui.activity.PhotoEditorBuilder;
import ly.img.android.pesdk.ui.model.state.UiConfigFilter;
import ly.img.android.pesdk.ui.model.state.UiConfigFrame;
import ly.img.android.pesdk.ui.model.state.UiConfigOverlay;
import ly.img.android.pesdk.ui.model.state.UiConfigSticker;
import ly.img.android.pesdk.ui.model.state.UiConfigText;
import ly.img.android.pesdk.ui.utils.PermissionRequest;
import ly.img.android.serializer._3.IMGLYFileWriter;

public class ImageEditingActivity extends AppCompatActivity implements PermissionRequest.Response {

    private static final String TAG = "ImageEditingActivity";
    public static final int PESDK_RESULT = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionRequest.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void permissionGranted() {}

    @Override
    public void permissionDenied() {
        Toast.makeText(this, "Permissions are required to edit images", Toast.LENGTH_SHORT).show();
        finish();
    }

    private PhotoEditorSettingsList createPesdkSettingsList() {
        PhotoEditorSettingsList settingsList = new PhotoEditorSettingsList();

        settingsList.getSettingsModel(UiConfigFilter.class).setFilterList(
                FilterPackBasic.getFilterPack()
        );

        settingsList.getSettingsModel(UiConfigText.class).setFontList(
                FontPackBasic.getFontPack()
        );

        settingsList.getSettingsModel(UiConfigFrame.class).setFrameList(
                FramePackBasic.getFramePack()
        );

        settingsList.getSettingsModel(UiConfigOverlay.class).setOverlayList(
                OverlayPackBasic.getOverlayPack()
        );

        settingsList.getSettingsModel(UiConfigSticker.class).setStickerLists(
                StickerPackEmoticons.getStickerCategory(),
                StickerPackShapes.getStickerCategory()
        );

        return settingsList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        if (uri == null) {
            String uriString = getIntent().getStringExtra("uri");
            if (uriString != null) {
                uri = Uri.parse(uriString);
            }
        }

        if (uri != null) {
            openEditor(uri);
        } else {
            Log.e(TAG, "No image URI provided");
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void openEditor(Uri inputImage) {
        PhotoEditorSettingsList settingsList = createPesdkSettingsList();

        settingsList.getSettingsModel(LoadSettings.class).setSource(inputImage);
        settingsList.getSettingsModel(PhotoEditorSaveSettings.class).setOutputToGallery(Environment.DIRECTORY_DCIM);

        new PhotoEditorBuilder(this)
                .setSettingsList(settingsList)
                .startActivityForResult(this, PESDK_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK && requestCode == PESDK_RESULT) {
            EditorSDKResult data = new EditorSDKResult(intent);
            data.notifyGallery(EditorSDKResult.UPDATE_RESULT & EditorSDKResult.UPDATE_SOURCE);

            Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show();

            if (getIntent().getBooleanExtra("return_result", false)) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("edited_uri", data.getResultUri().toString());
                resultIntent.putExtra("media_type", "image");
                setResult(RESULT_OK, resultIntent);
                finish();
            } else if (getIntent().hasExtra("type")){
                Intent i = new Intent(this, AddStoryActivity.class);
                i.putExtra("type", "image");
                i.putExtra("uri", data.getResultUri().toString());
                startActivity(i);
                finish();
            } else {
                Intent i = new Intent(this, MenuActivity.class);
                startActivity(i);
                finish();
            }

            SettingsList lastState = data.getSettingsList();
            try {
                new IMGLYFileWriter(lastState).writeJson(new File(
                        getExternalFilesDir(null),
                        "serialisation.json"
                ));
            } catch (Exception e) {
                Log.e(TAG, "Failed to write serialisation", e);
            }

        } else if (resultCode == RESULT_CANCELED && requestCode == PESDK_RESULT) {
            if (getIntent().getBooleanExtra("return_result", false)) {
                setResult(RESULT_CANCELED);
            }
            finish();
        }
    }
}
