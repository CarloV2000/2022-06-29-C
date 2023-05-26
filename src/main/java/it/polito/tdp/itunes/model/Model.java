package it.polito.tdp.itunes.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import it.polito.tdp.itunes.db.ItunesDAO;

public class Model {
	private Graph<Album, DefaultWeightedEdge>grafo;
	private List<Album>allAlbum;
	private Map<Integer, Album>albumIdMap;
	private ItunesDAO dao;
	private List<Album>migliore;
	private int nMigliore;
	
	public Model() {
		this.allAlbum = new ArrayList<Album>();
		this.albumIdMap = new HashMap<>();
		this.dao = new ItunesDAO();
	}
	
	public void creaGrafo(int costoMAX) {
		//creo grafo
		this.grafo = new SimpleWeightedGraph<Album, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		//vertici
		this.allAlbum = dao.getAllAlbumsConPrezzoInferiore(costoMAX);
		Graphs.addAllVertices(this.grafo, this.allAlbum);
		//idMap
		for(Album x : this.allAlbum) {
			this.albumIdMap.put(x.getAlbumId(), x);
		}
		//archi pesati
		for(Album x : this.allAlbum) {
			for(Album y: this.allAlbum) {
				
				if(!x.equals(y) && x.getPrezzo()-y.getPrezzo()!=0) {
					int peso = 0;
					if(x.getPrezzo() > y.getPrezzo())
						peso = x.getPrezzo()-y.getPrezzo();
					else if(x.getPrezzo() < y.getPrezzo())
						peso = y.getPrezzo()-x.getPrezzo();
					
					grafo.addEdge(x, y);
					grafo.setEdgeWeight(x, y, peso);
				}
			}
		}
		
	}
	
	/**
	 * Si definisca il “bilancio” di un vertice come la media di tutti i pesi dei suoi archi incidenti. Permettere all’utente di 
	 * selezionare, dall’apposita tendina, un album a1 tra quelli presenti nel grafo (elencati in ordine alfabetico di titolo). 
	 * Alla pressione del bottone “Stampa Adiacenze”, si si stampino tutti i nodi adiacenti di a1 in ordine decrescente di bilancio 
	 * (vedere gli screenshot alle pagine seguenti per un esempio di stampa corretto)
	 * @return
	 */
	public double getBilancio(Album a) {
		double bilancio = 0;
		int nArchi = 0;
		double bilancioMedio = 0.0;
		List<DefaultWeightedEdge>archiIncidenti = new ArrayList<>();
		archiIncidenti.addAll(grafo.incomingEdgesOf(a));
		archiIncidenti.addAll(grafo.outgoingEdgesOf(a));
		for(DefaultWeightedEdge x : archiIncidenti) {
			nArchi++;
			bilancio += grafo.getEdgeWeight(x);
		}
		bilancioMedio = bilancio / nArchi;
		return bilancioMedio;
	}
	/**
	 * metodo per creare lista di tutti gli album adacenti a quello dato ordinati per bilancio
	 * @param album1 è l'album inserito dall'utente
	 * @return
	 */
	public List<AlbumBilancio> getBilanci(Album album1){
		List<AlbumBilancio>result = new ArrayList<AlbumBilancio>();
		List<Album>albumsAdiacenti = new ArrayList<Album>(Graphs.neighborListOf(grafo, album1));
		for(Album x : albumsAdiacenti) {
			AlbumBilancio aB = new AlbumBilancio(x, getBilancio(x));
			result.add(aB);
		}
		Collections.sort(result);
		return result;
	}
	
	/**PUNTO 2. DA QUI INIZIA PARTE ERRATA
	 * Permettere all’utente di inserire una soglia numerica x, e di selezionare, dall’apposita tendina, un ulteriore album a2
	 *  tra quelli presenti nel grafo. Alla pressione del bottone “Calcola Percorso”, trovare e stampare (se esiste) 
	 *  un cammino semplice sul grafo calcolato nel punto 1 che abbia le seguenti caratteristiche:
			• parta da a1 (selezionato al punto 1d) e termini in a2;
			• attraversi solo archi con peso maggiore o uguale a x;
			• tocchi il maggior numero di vertici che hanno un “bilancio” maggiore di quello del vertice di partenza 
              a1 (per il calcolo del “bilancio” di un vertice si veda il punto 1d).
	 * @return
	 */
	public List<Album> calcolaCammino(Album a1, Album a2, int sogliaX) {
		migliore = new ArrayList<Album>();
		nMigliore = 0;
		List<Album> parziale = new ArrayList<>();
		cercaMeglio(parziale, 0, sogliaX, a1, a2);
		return migliore;
	}
	
	private void cercaMeglio(List<Album> parziale, int Livello, int sogliaX, Album a1, Album a2) {//cerco di fare ricorsione meglio
		
		if(!parziale.get(parziale.size()).equals(a2) || parziale.size() < 2)//l'ultimo album non è a2: esco
			return;
		
		else if(parziale.get(parziale.size()-1).equals(a2)){ //possibile avere soluzione in questo caso
			int nElementiConBilancioMaggioreMAX = this.nAlbumConBilancioMaggioreDiA1(parziale, a1);
			if(nElementiConBilancioMaggioreMAX > nMigliore) {
				nMigliore = nElementiConBilancioMaggioreMAX;
				migliore = new ArrayList<>(parziale);
			}
			return;
		}
		
		if(Livello == this.allAlbum.size())//se ho gia aggiunto tutti gli album della lista esco
			return;
		
		Album corrente = allAlbum.get(Livello);
		Album precedente =  allAlbum.get(Livello-1);
		DefaultWeightedEdge e = grafo.getEdge(precedente, corrente);
		//provo ad aggiungere prossimo elemento
		if(grafo.getEdgeWeight(e) >= sogliaX) {		
			parziale.add(corrente);
			cercaMeglio(parziale, Livello+1, sogliaX, a1, a2);
			parziale.remove(corrente);
		}

	}


	public int nAlbumConBilancioMaggioreDiA1(List<Album>parziale, Album a1) {
		int n = 0;
		for(Album x : parziale) {
			if(getBilancio(x) > getBilancio(a1)) {
				n++;
			}
		}
		return n;
	}

	public Graph<Album, DefaultWeightedEdge> getGrafo() {
		return grafo;
	}

	public List<Album> getAllAlbum() {
		return allAlbum;
	}

	public ItunesDAO getDao() {
		return dao;
	}
	
	
	
	
}
