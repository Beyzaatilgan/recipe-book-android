package com.beyzaatilgan.yemekkitabi2.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.room.Room
import com.beyzaatilgan.yemekkitabi2.databinding.FragmentTarifBinding
import com.beyzaatilgan.yemekkitabi2.model.Tarif
import com.beyzaatilgan.yemekkitabi2.roomdb.TarifDAO
import com.beyzaatilgan.yemekkitabi2.roomdb.TarifDatabase
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream


class TarifFragment : Fragment() {

    private var _binding: FragmentTarifBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionLauncher: ActivityResultLauncher<String>//izin istemek için
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>//galeriye gitmek için
    private var secilenGorsel : Uri?=null // bizim resourcenin kaynağın yerini belirtir.
    private var secilenBitmap : Bitmap?=null //uri bilgisini görsele çeviren bir sınıf bitmap.

    private val mDisposable = CompositeDisposable() //çok fazla istek yapıldığında bunlar hafızada birikmesin diye gerektiğinde kullan at çöpü görevi görür.
    private lateinit var db: TarifDatabase
    private lateinit var tarifDao: TarifDAO
    private var secilenTarif : Tarif? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()//bunu çağırıyorum.

        db= Room.databaseBuilder(requireContext(),TarifDatabase::class.java,"Tarifler").build()
        tarifDao= db.tarifDao()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTarifBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // bu binding işlemlerini fragmentda yapmak zorunluydu zaten asagıdaki fonksiyonların tek amacı düzenli yazmak oluyor.

        binding.imageView.setOnClickListener{gorselSec(it)}
        binding.kaydetButton.setOnClickListener{kaydet(it)}
        binding.silButton.setOnClickListener{sil(it)}

        arguments?.let {
            val bilgi = TarifFragmentArgs.fromBundle(it).bilgi
            if (bilgi == "yeni") {
                //yeni tarif ekleniyor.
                secilenTarif = null
                binding.silButton.isEnabled = false
                binding.kaydetButton.isEnabled = true
                binding.isimText.setText("")
                binding.malzemeText.setText("")
            } else {
                //eski eklenmiş tarif gösteriliyor.
                binding.silButton.isEnabled = true
                binding.kaydetButton.isEnabled = false
                val id = TarifFragmentArgs.fromBundle(it).id
                mDisposable.add(
                    tarifDao.findById(id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResponse)
                )

            }


        }
    }
    private fun handleResponse(tarif: Tarif){
        val bitmap = BitmapFactory.decodeByteArray(tarif.gorsel,0,tarif.gorsel.size)
        binding.imageView.setImageBitmap(bitmap)
        binding.isimText.setText(tarif.isim)
        binding.malzemeText.setText(tarif.malzeme)
        secilenTarif = tarif

    }


    fun kaydet(view: View){
        val isim = binding.isimText.text.toString()
        val malzeme = binding.malzemeText.text.toString()
        if (secilenBitmap != null){
            val kucukBitmap = kucukBitmapOlustur(secilenBitmap!!,300)
            val outputStream = ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteDizisi = outputStream.toByteArray()

            val tarif = Tarif(isim,malzeme,byteDizisi)


            //Threading; uygulamanın çökmemesi için yapılır.DAO kısmında Flowable ve completeable yapılır.
            //RxJava
            mDisposable.add(
                tarifDao.insert(tarif)
                .subscribeOn(Schedulers.io()) //io,veritabanı okuma internete gitme gibi işlemleri arka planda yapar.computation,fazla veri hesaplamalarında vs CPU ya yüklenir.
                .observeOn(AndroidSchedulers.mainThread()) //observeOn, arka planda alınan verinin nerede gösterileceğini ayarlar.
                .subscribe(this::handleResponseForInsert) // sonucunda ne olacağını buraya atayabiliyorum.)
            )
        }

    }
    private fun handleResponseForInsert(){
        //bittiğinde bir önceki fragmenta dön.
        val action = TarifFragmentDirections.actionTarifFragmentToListeFragment()
        Navigation.findNavController(requireView()).navigate(action)

    }

    fun sil(view: View){
        if (secilenTarif != null){
            mDisposable.add(
                tarifDao.delete(tarif = secilenTarif!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponseForInsert)
            )
        }

    }
    fun gorselSec(view: View){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

            //izinin daha önce alınıp alınmadığını kontrol eden fonksiyon.
            //önce izni kontrol eder sonra izin var mı yok mu onu kontrol eder.
            if (ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                //izin verilmemiş izin istememizi istiyor durumu.
                //kullanıcı yanlışlıkla izni vermemiş olabilir diye tekrar gösterip göstermemeye android karar veriyor.
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.READ_MEDIA_IMAGES)){
                    //snackbar göstermemiz lazım.Kullanıcıdan neden izin istediğimizi söyleyerek tekrar istememiz gerek.
                    //kullanıcı bir şeye tıklamadan gitmez.
                    Snackbar.make(view,"Galeriye ulaşıp görsel seçmemiz lazım!",Snackbar.LENGTH_INDEFINITE).setAction("izin ver.", View.OnClickListener {
                        //izin isteyeceğiz.
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)

                    }).show()

                }else{
                    //direkt sadece izin isteyeceğiz.
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }else{
                //izin verilmiş galeriye gidebilirim.
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }

        }else{
            //izinin daha önce alınıp alınmadığını kontrol eden fonksiyon.
            //önce izni kontrol eder sonra izin var mı yok mu onu kontrol eder.
            if (ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //izin verilmemiş izin istememizi istiyor durumu.
                //kullanıcı yanlışlıkla izni vermemiş olabilir diye tekrar gösterip göstermemeye android karar veriyor.
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.READ_EXTERNAL_STORAGE)){
                    //snackbar göstermemiz lazım.Kullanıcıdan neden izin istediğimizi söyleyerek tekrar istememiz gerek.
                    //kullanıcı bir şeye tıklamadan gitmez.
                    Snackbar.make(view,"Galeriye ulaşıp görsel seçmemiz lazım!",Snackbar.LENGTH_INDEFINITE).setAction("izin ver.", View.OnClickListener {
                        //izin isteyeceğiz.
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                    }).show()

                }else{
                    //direkt sadece izin isteyeceğiz.
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }else{
                //izin verilmiş galeriye gidebilirim.
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }

        }





    }

    //Launcherları kaydetme işlemleri burada yapılacak.
    private fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK){
                val intentFromResult = result.data
                if (intentFromResult != null){
                    secilenGorsel = intentFromResult.data

                    try {
                        //bu yeni yöntem.
                        if (Build.VERSION.SDK_INT >= 28){
                            val source = ImageDecoder.createSource(requireActivity().contentResolver,secilenGorsel!!)
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(secilenBitmap)
                        }else{
                            //bu eski yöntem.
                            secilenBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver,secilenGorsel)
                            binding.imageView.setImageBitmap(secilenBitmap)
                        }

                    }catch (e: Exception){
                        println(e.localizedMessage)

                    }

                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result ->
            if (result){
                //izin verildi.galeriye gidebiliriz.
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }else{
                //izin verilmedi.
                Toast.makeText(requireContext(), "izin verilmedi!", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun kucukBitmapOlustur(kullanicininSectigiBitmap : Bitmap, maximumBoyut: Int) : Bitmap{
        var width = kullanicininSectigiBitmap.width
        var height = kullanicininSectigiBitmap.height
        val bitmapOrani : Double = width.toDouble() / height.toDouble()

        if (bitmapOrani > 1){
            //gorsel yatay
            width = maximumBoyut
            val kisaltilmisYukseklik = width / bitmapOrani
            height = kisaltilmisYukseklik.toInt()
        }else{
            //gorsel dikey
            height = maximumBoyut
            val kisaltilmisGenislik = height * bitmapOrani
            width = kisaltilmisGenislik.toInt()
        }

        return Bitmap.createScaledBitmap(kullanicininSectigiBitmap,width,height,true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposable.clear()
    }
}