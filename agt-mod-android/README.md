# AGT MOD — aplicativo Android

Primeira estrutura nativa do aplicativo AGT MOD.

## O que esta versão contém

- Projeto Android nativo em Kotlin.
- Tela de entrada/cadastro usando Supabase Auth do AGT Arena.
- Abertura do Slither.io em uma WebView dentro do próprio aplicativo.
- Camada visual inicial reservada para o radar.
- Restrições de navegação da WebView do jogo ao domínio slither.io.

## Limitações conhecidas

A camada do radar ainda é apenas visual. Esta versão não lê posições de jogadores, cobras, mapa ou estado interno da partida. O Slither.io não foi integrado por API oficial nesta etapa e o aplicativo não injeta scripts no jogo. Para ativar um radar real, será necessário validar uma interface de integração permitida e tecnicamente disponível.

O login usa a chave pública publishable do Supabase. Nunca colocar uma chave `service_role` em um APK. O cadastro pode exigir confirmação de e-mail conforme a configuração de Auth do projeto Supabase.

## Gerar APK debug

Requer JDK 17 e Android SDK (API 36).

```bash
gradle assembleDebug
```

O APK será gerado em:

```
app/build/outputs/apk/debug/app-debug.apk
```
