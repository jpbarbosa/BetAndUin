|-------------------------------------------------------------------------------------------------|
|>>>>>>>>>>>>>>>>>>>>>>>>INSTRU��ES DE INICIA��O DAS APLICA��ES<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<|
|-------------------------------------------------------------------------------------------------|
Para iniciar um servidor:

	java -jar [fileName] [serverNumber] [partnerIpAddress] -debugging
	
		[serverNumber]		    Corresponde ao n�mero do servidor, de modo a serem
								carregadas as defini��es de portos correctas.
								O primeiro servidor � o servidor prim�rio por defeito
								enquanto o segundo � considerado secund�rio.
								Para este campo, digitar 1 ou 2 conforme se pretende
								activar o primeiro ou segundo servidor.
								N�o s�o permitidas repeti��es dos n�meros, isto �,
								n�o se pode iniciar simulataneamente mais do que um
								servidor com o mesmo n�mero.
						
		[partnerIpAddress]  	Corresponde ao endere�o IP do outro servidor com o qual
								este servidor vai interagir.
						
		-debugging				'Flag' opcional que obriga o servidor a ir imprimindo
								mensagens de 'debugging' ao longo da execu��o, como o
								tipo de mensagens que envia, recebe, a que portos se est�
								a ligar e quando ocorre 'timeouts', por exemplo.
						
						
Para iniciar um cliente, tanto como TCP como RMI:

	java -jar [fileName] [firstServerIpAddress] [secondServerIpAddress]
	
		[firstServerIpAddress]  Corresponde ao endere�o IP ao qual este cliente ir� tentar
								ligar-se em primeiro lugar. De uma forma geral, aconselha-se
								que este endere�o seja igual ao do servidor prim�rio por
								defeito (servidor 1).
						
		[secondServerIpAddress] Corresponde ao endere�o IP do segundo servidor, sendo recomendado
								que corresponda ao endere�o do servidor 2.
								
								
|-------------------------------------------------------------------------------------------------|								
|>>>>>>>>>>>>>>>>>>>>>>>>INSTRU��ES CONFIGURA��O DAS APLICA��ES<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<|
|-------------------------------------------------------------------------------------------------|

No ficheiro de configura��o 'properties.conf', encontram-se os seguintes campos:
	
	-> NO_RETRIES
		N�mero de tentativas para se ligar a um servidor que um cliente efectua antes te tentar
		o segundo servidor ou ent�o desistir.
	
	-> CLIENT_WAITING_TIME
		Tempo de espera entre duas tentativas consecutivas de se ligar a um servidor.
			
	-> DEFAULT_CREDITS
		N�mero de cr�ditos que s�o dados por defeito a um cliente. Este n�mero tamb�m � usado
		quando um cliente efectua um 'reset'.

	-> BUFFER_SIZE
		Tamanho do 'buffer' que guardas as mensagens enquanto a liga��o est� em baixo.
	
	-> TIME_BETWEEN_ROUNDS
		Tempo de dura��o de uma ronda de jogos.
			
	-> NO_GAMES
		N�mero de jogos por ronda.
						
	-> SERVER_INIT_RETRIES
		N�mero m�ximo de mensagens 'I_WILL_BE_PRIMARY_SERVER' que um servidor envia ao iniciar,
		antes de considerar o outro servidor desligado.
	
	-> FIRST_TCP_SERVER_PORT
		Porto do primeiro servidor ao qual os clientes TCP se ligam.
	
	-> SECOND_TCP_SERVER_PORT
		Porto do segundo servidor ao qual os clientes TCP se ligam.
	
	-> FIRST_RMI_SERVER_PORT
		Porto em que o primeiro servidor regista os seus objectos remotos.
			
	-> SECOND_RMI_SERVER_PORT
		Porto em que o segundo servidor regista os seus objectos remotos.
			
	-> STONITH_FIRST_SERVER_PORT
		Porto ao qual o segundo servidor se liga de modo a simular a liga��o STONITH.
				
	-> STONITH_SECOND_SERVER_PORT
		Porto ao qual o primeiro servidor se liga de modo a simular a liga��o STONITH.
	
	-> SERVER_WAITING_TIME
		Tempo para que o servidor considere o seu companheiro desactivado. Se ao fim deste
		per�odo o servidor n�o receber nenhuma mensagem vinda do outro servidor, ocorre um
		'timeout' e o servidor inicia os procedimentos adequados.
		
	-> FIRST_WAITING_TIME
		Tempo de espera entre o envio de duas mensagens 'I_WILL_BE_PRIMARY_SERVER' no in�cio
		do estabelecimento da liga��o, caso n�o tenha obtido resposta � primeira.
		
	-> KEEP_ALIVE_TIME
		Tempo entre o envio de duas mensagens 'KEEP_ALIVE'. Naturalmente, tem de ser maior
		do que SERVER_WAITING_TIME.
	

Por defeito, s�o feitas as seguintes configura��es:

NO_GAMES=10
BUFFER_SIZE=10
SECOND_TCP_SERVER_PORT=7000
STONITH_FIRST_SERVER_PORT=8000
SERVER_WAITING_TIME=15000
FIRST_WAITING_TIME=5000
KEEP_ALIVE_TIME=5000
SERVER_INIT_RETRIES=3
NO_RETRIES=10
DEFAULT_CREDITS=100
CLIENT_WAITING_TIME=1000
FIRST_TCP_SERVER_PORT=6000
SECOND_RMI_SERVER_PORT=13000
TIME_BETWEEN_ROUNDS=60000
STONITH_SECOND_SERVER_PORT=9000
FIRST_RMI_SERVER_PORT=12000