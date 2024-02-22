# IRC-p3proj
<a name="readme-top"></a>
<!-- PROJECT SHIELDS -->
[![Licence](https://img.shields.io/github/license/Ileriayo/markdown-badges?style=for-the-badge)](./LICENSE)
![GitHub][GitHub.com]

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/r3aprz/net9project">
    <img src="latex_source/imagens/logoREADME.png" alt="Logo" width="200" height="200">
  </a>

  <h3 align="center">IRC Chat</h3> 
  <br>

  <p align="center">
    Reti di Calcolatori (9 CFU)
    <br />
    A.A. 2023/24
    <br>

  </p>
</div>

<!-- Traccia -->
# Traccia
Simulare una chat multiutente	basata su IRC. Utilizzare un approccio client/server.<br>

### Server:
* Permette agli utenti di connettersi
* Mostra agli utenti una serie di possibili canali attivi (identificati con #)
* Rappresenta un gruppo in cui tutti gli utenti connessi possono inviare messaggi visibili a tutti coloro che sono connessi in quel canale
* Permette all'utente di cambiare il canale su cui è connesso
* Gestisce la collisione tra nomi utenti uguali
* Permette a due utenti di parlare in privato

### Client:
* Si connette ad un server specificando un nome utente, non è richiesta la password
* Può richiedere la lista dei canali inviando
  * Comando: `/list`
* Può connettersi ad un canale
  * Comando: `/join #channel_name`
* Può vedere gli utenti connessi
  * Comando: `/users`
* Può inviare messaggi
  * Comando `/msg messaggio`
* Può inviare un messaggio privat ad un utente
  * Comando: `/privmsg nickname messaggio`
* Può cambiare il canale su cui è connesso in qualunque momento

Implementare l'utente <i><b>amministratore</b></i> che può:
* Espellere un utente dal canale:
  * Comando: `/kick nickname`
* Bannare/sbannare un utente dal canale:
  * Comando: `/ban nickname`
  * Comando: `/unban nickname`
* Promuovere un utente come moderatore:
  * Comando: `/promote nickname`

<!-- ABOUT THE PROJECT -->
## About The Project

<div style="text-align: justify">
   La seguente proposta per il progetto di Reti di Calcolatori è un'applicazione di chat IRC scritta
   in Java. <br> Questo progetto offre un'esperienza completa di sviluppo in Java, includendo
   la gestione delle connessioni di rete (tramite architettura client/server), accesso al server, canali etc.
   Mette a disposizione una completa piattaforma corredata di comandi di ogni genere, in base al privilegio utente.
</div>
<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- BUILD WITH -->
## Built With

### Back and Front:

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)


<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Starting the Application

1. Eseguire per prima cosa il server, che si trova al seguente percorso: `src/ChatServer.java`
2. Eseguire più istanze del cliente, che si trova al seguente percorso: `src/ChatClient.java`
3. Inserire quindi tramite l'interfaccia da terminale appena aperta un nome utente (che deve essere univoco) per accedere al server
4. Iniziare a mesaggiare!

<!-- LICENSE -->
## License
> [!WARNING]
> Distributed under the `MIT License`. See <a href="https://github.com/checcafor/IRC-p3proj/blob/main/LICENSE">LICENSE</a> for more information.
<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTACT -->
## Contact
> [!NOTE]
> De Micco Francesco - [linkedin](https://www.linkedin.com/in/francesco-de-micco-b55034210/) - francesco.demicco001@studenti.uniparthenope.it <br>
> Formisano Francesca - [linkedin](https://www.linkedin.com/in/francesca-formisano-056460263/) - francesca.formisano001@studenti.uniparthenope.it <br>
>
> Project Link: [https://github.com/r3aprz/net9project](https://github.com/r3aprz/net9project)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
[GitHub.com]: https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white
