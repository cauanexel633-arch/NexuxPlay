# Game Hub — Console Launcher Estilo PlayStation 5

Aplicativo Android desenvolvido em **Kotlin**, **Jetpack Compose** (Material 3 customizado) e **minSdk 26** que transforma o smartphone/tablet em uma experiência de console inspirada no **PlayStation 5**.

---

## Funcionalidades Implementadas

1. **Tela de Carregamento (Splash) e Lançamento de Jogos**
   - Ao abrir o aplicativo, exibe splash animada com o logotipo **GAME HUB**, símbolos geométricos estilo console e barra de progresso.
   - Ao abrir um jogo da biblioteca, exibe tela de carregamento dedicada com a **capa (ícone do app)** e **nome do jogo**, executando a otimização de RAM caso configurada/confirmada antes de chamar `packageManager.getLaunchIntentForPackage`.

2. **Interface Estilo PS5**
   - Fundo azul-marinho escuro com gradiente dinâmico e iluminação atmosférica.
   - Barra superior com hora em tempo real, indicador de controle conectado, status de conexão (Wi-Fi / Dados / Offline) e porcentagem da bateria.
   - Navegação por abas: **Biblioteca**, **Loja/Destaques**, **Desempenho**, **Gerenciar Jogos** e **Configurações**.
   - Carrossel horizontal de jogos com efeito de **zoom e brilho** ao selecionar.
   - Seleção personalizada de jogos via `PackageManager` com checkboxes em **Gerenciar Jogos**, persistida em **Jetpack DataStore Preferences**.

3. **Compatibilidade Total com Controles (Gamepad Bluetooth / USB)**
   - Suporte a controles **Xbox**, **PS4 DualShock**, **PS5 DualSense** e gamepads USB/BT genéricos via `KeyEvent` e `MotionEvent`.
   - Mapeamento:
     - **D-Pad / Analógico Esquerdo**: Navegar entre abas, cards e listas
     - **Botão A / ✕ (Cross)**: Selecionar item ou iniciar jogo
     - **Botão B / ◯ (Circle)**: Voltar para a Biblioteca ou fechar diálogo
     - **Start / Options**: Abrir aba Configurações
     - **Select / Share**: Abrir aba Desempenho
     - **L1 / R1 (LB / RB)**: Alternar abas rapidamente
   - Indicador visual de controle conectado em tempo real via `InputManager.InputDeviceListener`.

4. **Otimização de Desempenho**
   - Monitoramento em tempo real de **CPU (%)**, **RAM (usada/livre)** e **Temperatura (°C)**.
   - Lista de processos/apps em segundo plano com checkboxes para o usuário **desmarcar os que não deseja fechar**.
   - Botão **Otimizar Agora** que limpa cache interno e chama `ActivityManager.killBackgroundProcesses` apenas para os apps de usuário selecionados (nunca encerra o próprio app nem serviços do sistema).
   - Verificação automática configurável ao abrir jogos: sugere otimização caso a RAM livre esteja abaixo do limite configurado.
   - Tratamento completo de `PACKAGE_USAGE_STATS` ausente com guia e atalho direto para `Settings.ACTION_USAGE_ACCESS_SETTINGS`.

5. **Inicialização Automática (`BOOT_COMPLETED`)**
   - `BootReceiver` registrado para abrir o aplicativo ao ligar o aparelho.
   - Guia integrado na tela inicial e em Configurações explicando como liberar o "Iniciar automaticamente" em fabricantes como **Xiaomi (MIUI/HyperOS)**, **Samsung (One UI)** e **Motorola**.

---

## Lista de Permissões Utilizadas (`AndroidManifest.xml`)

| Permissão | Finalidade | Observação |
| :--- | :--- | :--- |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Permite ao `BootReceiver` abrir o Game Hub ao ligar o aparelho. | Em Xiaomi, Samsung e Motorola pode exigir liberação manual de "Início automático" / "Bateria irrestrita". |
| `android.permission.KILL_BACKGROUND_PROCESSES` | Permite encerrar processos em segundo plano não essenciais sem root via `ActivityManager.killBackgroundProcesses`. | Aplicativos do sistema e o próprio Game Hub são protegidos por filtro estrito. |
| `android.permission.ACCESS_NETWORK_STATE` | Lê o estado de Wi-Fi / Dados Móveis para a barra superior do console. | Permissão normal concedida na instalação. |
| `android.permission.PACKAGE_USAGE_STATS` | Permite consultar apps recentes em segundo plano via `UsageStatsManager`. | Permissão especial: se não concedida, o app guia o usuário até **Acesso de Uso** nas Configurações do Android. |
| `<queries>` (`MAIN` + `LAUNCHER`) | Declara visibilidade dos aplicativos lançáveis instalados no Android 11+ via `PackageManager`. | Evita o uso amplo de `QUERY_ALL_PACKAGES`. |

---

## Instruções para Gerar o APK (Build)

### Pelo Android Studio
1. Abra a pasta raiz do projeto no **Android Studio** (versão Ladybug ou superior recomendada, JDK 11+).
2. Aguarde a sincronização automática do Gradle.
3. No menu superior, acesse **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Ao finalizar, clique em **locate** na notificação para encontrar o arquivo em `app/build/outputs/apk/debug/app-debug.apk`.
5. Para gerar APK de produção assinado, use **Build → Generate Signed Bundle / APK...** selecionando **APK**.

### Pela Linha de Comando (Gradle)
Execute na raiz do projeto:

```bash
# Gerar APK de Debug
gradle :app:assembleDebug

# Gerar APK de Release (requer variáveis de keystore configuradas)
gradle :app:assembleRelease
```

O APK de debug será gerado em:
`app/build/outputs/apk/debug/app-debug.apk`
