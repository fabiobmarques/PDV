package br.com.training.pdv.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;

import com.mapzen.android.lost.api.LocationServices;
import com.mapzen.android.lost.api.LostApiClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.com.training.pdv.R;
import br.com.training.pdv.domain.model.Produto;
import br.com.training.pdv.domain.util.Base64Util;
import br.com.training.pdv.domain.util.ImageInputHelper;
import butterknife.Bind;
import butterknife.OnClick;
import dmax.dialog.SpotsDialog;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import se.emilsjolander.sprinkles.Query;

public class EditarProdutoActivity extends BaseActivity implements ImageInputHelper.ImageActionListener{

    @Bind(R.id.spinner)
    Spinner spinner;
    @Bind(R.id.editTextDescricao)
    EditText editTextDescricao;
    @Bind(R.id.editTextUnidade)
    EditText editTextUnidade;
    @Bind(R.id.editTextPreco)
    EditText editTextPreco;
    @Bind(R.id.editTextCodigo)
    EditText editTextCodigo;

    @Bind(R.id.imageViewFoto)
    ImageView imageViewFoto;
    @Bind(R.id.imageButtonCamera)
    ImageButton imageButtonCamera;
    @Bind(R.id.imageButtonGaleria)
    ImageButton imageButtonGaleria;

    private ImageInputHelper imageInputHelper;

    private Produto produto;

    private Callback<String> callbackEditaProduto;

    private AlertDialog dialog;

    private double latitude = 0.0d;
    private double longitude = 0.0d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_produto);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        configureEditaProdutoCallback();
        dialog = new SpotsDialog(this,"Atualizando servidor");

        LostApiClient lostApiClient = new LostApiClient.Builder(this).build();
        lostApiClient.connect();

        Location location = LocationServices.FusedLocationApi.getLastLocation();
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }

        Log.d("LOCATION","Editar Latitude:"+latitude);
        Log.d("LOCATION","Editar Longitude:"+longitude);


        imageInputHelper = new ImageInputHelper(this);
        imageInputHelper.setImageActionListener(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                produto.setDescricao(editTextDescricao.getText().toString());
                produto.setUnidade(editTextUnidade.getText().toString());
                produto.setCodigoBarras(editTextCodigo.getText().toString());
                if(!editTextPreco.getText().toString().equals("")){
                    produto.setPreco(Double.parseDouble(editTextPreco.getText().toString()));
                }
                Bitmap imagem = ((BitmapDrawable)imageViewFoto.getDrawable()).getBitmap();
                produto.setFoto(Base64Util.encodeTobase64(imagem));

                produto.setLatitude(latitude);
                produto.setLongitude(longitude);

                produto.save();

                Snackbar.make(view,"Produto Alterado com Sucesso!",Snackbar.LENGTH_SHORT).show();

            }
        });

        List<Produto> produtos = Query.many(Produto.class,"select * from produto order by codigo_barra").get().asList();

        produto = new Produto();

        List<String> barcodeList = new ArrayList<>();

        for(Produto produto: produtos){
            barcodeList.add(produto.getCodigoBarras());
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item,barcodeList);

        dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(dataAdapter);


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String barcode = parent.getItemAtPosition(position).toString();

                Log.d("BARCODE","Selecionado-->"+barcode);

                produto = Query.one(Produto.class,"select *from produto where codigo_barra = ?",barcode).get();
                if(produto!=null){

                    editTextDescricao.setText(produto.getDescricao());
                    editTextUnidade.setText(produto.getUnidade());
                    editTextCodigo.setText(produto.getCodigoBarras());
                    editTextPreco.setText(String.valueOf(produto.getPreco()));

                    imageViewFoto.setImageBitmap(Base64Util.decodeBase64(produto.getFoto()));

                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    @OnClick(R.id.imageButtonGaleria)
    public void onClickGaleria(){
        imageInputHelper.selectImageFromGallery();
    }

    @OnClick(R.id.imageButtonCamera)
    public void onClickCamera(){
        imageInputHelper.takePhotoWithCamera();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imageInputHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onImageSelectedFromGallery(Uri uri, File imageFile) {
        // cropping the selected image. crop intent will have aspect ratio 16/9 and result image
        // will have size 800x450
        imageInputHelper.requestCropImage(uri, 100, 100, 0, 0);
    }

    @Override
    public void onImageTakenFromCamera(Uri uri, File imageFile) {
        // cropping the taken photo. crop intent will have aspect ratio 16/9 and result image
        // will have size 800x450
        imageInputHelper.requestCropImage(uri, 100, 100, 0, 0);
    }

    @Override
    public void onImageCropped(Uri uri, File imageFile) {
        try {
            // getting bitmap from uri
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            imageViewFoto.setImageBitmap(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Método chamado para configurar o listen do retrofit
    //Verifica o retorno da requisicao HTTP ao webservice
    private void configureEditaProdutoCallback() {

        callbackEditaProduto = new Callback<String>() {

            @Override public void success(String resultado, Response response) {
                dialog.dismiss();
                //Finaliza a Activity
                finish();

            }

            @Override public void failure(RetrofitError error) {
                dialog.dismiss();

                Snackbar.make(findViewById(android.R.id.content).getRootView(),"Houve um problema de conexão! Por favor verifique e tente novamente!", Snackbar.LENGTH_SHORT).show();
                Log.e("RETROFIT", "Error:"+error.getMessage());
            }
        };
    }

}
