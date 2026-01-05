package com.beyzaatilgan.yemekkitabi2.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.beyzaatilgan.yemekkitabi2.databinding.RcyclerRowBinding
import com.beyzaatilgan.yemekkitabi2.model.Tarif
import com.beyzaatilgan.yemekkitabi2.view.ListeFragmentDirections

class TarifAdapter(val tarifListesi : List<Tarif> ) : RecyclerView.Adapter<TarifAdapter.TarifHolder>() {
    class TarifHolder(val binding: RcyclerRowBinding) : RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarifHolder {
        val recyclerRowBinding = RcyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return TarifHolder(recyclerRowBinding)
    }

    override fun getItemCount(): Int {
        return tarifListesi.size //kaç tarif varsa ona göre oluşturulur.
    }

    override fun onBindViewHolder(holder: TarifHolder, position: Int) {
        holder.binding.rcyclerViewTextView.text = tarifListesi[position].isim
        holder.itemView.setOnClickListener{
            val action = ListeFragmentDirections.actionListeFragmentToTarifFragment(bilgi = "eski",id = tarifListesi[position].id)
            Navigation.findNavController(it).navigate(action)
        }
    }
}