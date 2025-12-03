// Dados da aplicação
let customerName = '';
let cart = [];
let orders = [];
let products = [];

// Inicialização
document.addEventListener('DOMContentLoaded', function() {
    initDarkMode();
    loadAppData();
    setupAutoSave();
    aplicarMascaras();
    inicializarEventListeners();
    carregarProdutosDoBanco();
});

// Funções de inicialização
function loadAppData() {
    // Carregar dados do localStorage apenas como fallback
    const savedProducts = localStorage.getItem('cantina-products');
    const savedOrders = localStorage.getItem('cantina-orders');
    const savedCart = localStorage.getItem('cart');
    const savedCustomerName = localStorage.getItem('customerName');
    
    // Produtos serão carregados do banco, usar local apenas se banco falhar
    if (savedProducts && products.length === 0) {
        products = JSON.parse(savedProducts);
    }
    
    if (savedOrders) {
        orders = JSON.parse(savedOrders);
    }
    
    if (savedCart) {
        cart = JSON.parse(savedCart);
    }
    
    if (savedCustomerName) {
        customerName = savedCustomerName;
    }
}

function setupAutoSave() {
    // Salvar dados no localStorage periodicamente
    setInterval(() => {
        localStorage.setItem('cantina-products', JSON.stringify(products));
        localStorage.setItem('cantina-orders', JSON.stringify(orders));
        localStorage.setItem('cart', JSON.stringify(cart));
        localStorage.setItem('customerName', customerName);
    }, 2000);
}

// ========== FUNÇÕES DO CLIENTE ==========
function setCustomerName() {
    const nameInput = document.getElementById('customer-name');
    const name = nameInput.value.trim();
    
    if (name === '') {
        showAlert('Por favor, digite seu nome', 'error');
        return;
    }
    
    if (name.length < 2) {
        showAlert('Nome deve ter pelo menos 2 caracteres', 'error');
        return;
    }
    
    // LIMPAR DADOS ANTIGOS ANTES DE SETAR NOVO NOME
    localStorage.removeItem('cart');
    localStorage.removeItem('lastOrderCode');
    localStorage.removeItem('lastOrderSummary');
    
    customerName = name;
    localStorage.setItem('customerName', customerName);
    
    // FORÇAR ATUALIZAÇÃO IMEDIATA
    setTimeout(() => {
        window.location.href = 'produtos.html';
    }, 100);
}

function loadProducts() {
    const productsGrid = document.getElementById('products-grid');
    if (!productsGrid) return;
    
    productsGrid.innerHTML = '';
    
    const activeProducts = products.filter(p => p.ativo);

    if (activeProducts.length === 0) {
        productsGrid.innerHTML = '<p>Nenhum produto disponível no momento.</p>';
        return;
    }
    
    activeProducts.forEach(product => {
        const productCard = document.createElement('div');
        productCard.className = 'product-card';

        const imageElement = product.image
            ? `<img src="/uploads/${product.image}" alt="${product.name}" class="product-image">`
            : `<div class="product-image placeholder">${product.name.charAt(0)}</div>`;

        productCard.innerHTML = `
            ${imageElement}
            <div class="product-info">
                <div class="product-title">${product.name}</div>
                <div class="product-price">R$ ${product.price.toFixed(2)}</div>
                <div class="product-description">${product.description}</div>
                <button onclick="addToCart(${product.id})" class="btn btn-primary" style="width: 100%; margin-top: 10px;">Adicionar</button>
            </div>
        `;
        productsGrid.appendChild(productCard);
    });
}

function addToCart(productId) {
    const product = products.find(p => p.id === productId);
    if (!product) {
        showAlert('Produto não encontrado', 'error');
        return;
    }

    const existingItem = cart.find(item => item.product.id === productId);
    
    if (existingItem) {
        if (existingItem.quantity >= 10) {
            showAlert('Limite máximo de 10 unidades por produto', 'error');
            return;
        }
        existingItem.quantity++;
    } else {
        if (cart.length >= 10) {
            showAlert('Limite máximo de 10 itens diferentes no carrinho', 'error');
            return;
        }
        cart.push({
            product: product,
            quantity: 1
        });
    }
    
    localStorage.setItem('cart', JSON.stringify(cart));
    showAlert(`${product.name} adicionado ao carrinho!`, 'success');
}

function updateCart() {
    const cartItems = document.getElementById('cart-items');
    const cartTotal = document.getElementById('cart-total');
    
    if (!cartItems || !cartTotal) return;
    
    cartItems.innerHTML = '';
    let total = 0;
    
    if (cart.length === 0) {
        cartItems.innerHTML = '<p>Seu carrinho está vazio</p>';
        cartTotal.textContent = 'Total: R$ 0,00';
        return;
    }
    
    cart.forEach(item => {
        const itemTotal = item.product.price * item.quantity;
        total += itemTotal;
        
        const cartItem = document.createElement('div');
        cartItem.className = 'cart-item';
        cartItem.innerHTML = `
            <div>
                <strong>${item.product.name}</strong>
                <div>R$ ${item.product.price.toFixed(2)} x ${item.quantity}</div>
            </div>
            <div>
                R$ ${itemTotal.toFixed(2)}
                <button onclick="removeFromCart(${item.product.id})" style="margin-left: 10px;">✕</button>
            </div>
        `;
        cartItems.appendChild(cartItem);
    });
    
    cartTotal.textContent = `Total: R$ ${total.toFixed(2)}`;
}

function removeFromCart(productId) {
    cart = cart.filter(item => item.product.id !== productId);
    localStorage.setItem('cart', JSON.stringify(cart));
    updateCart();
    showAlert('Item removido do carrinho', 'success');
}

async function processPayment() {
    if (cart.length === 0) {
        showAlert('Seu carrinho está vazio!', 'error');
        return;
    }
    
    const total = cart.reduce((sum, item) => sum + (item.product.price * item.quantity), 0);
    if (total < 5.00) {
        showAlert('Valor mínimo do pedido é R$ 5,00', 'error');
        return;
    }

    try {
        showAlert('Processando pedido...', 'info');

        const pedidoData = {
            clienteNome: customerName,
            metodoPagamento: 'Dinheiro',
            itens: cart.map(item => ({
                produtoId: item.product.id,
                quantidade: item.quantity
            }))
        };

        const response = await fetch('http://localhost:8080/api/pedidos', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(pedidoData)
        });

        if (!response.ok) {
            throw new Error(`Erro HTTP: ${response.status}`);
        }

        const data = await response.json();

        if (data.success) {
            // 🧹 LIMPEZA APÓS COMPRA CONCLUÍDA
            const sessaoId = localStorage.getItem('sessaoId');
            if (sessaoId) {
                await finalizarSessao(sessaoId);
            }
            
            // Limpar carrinho local
            cart = [];
            localStorage.removeItem('cart');
            
            showAlert('Pedido realizado com sucesso!', 'success');
            
            // Preparar resumo para confirmação
            const orderCode = data.codigo;
            let orderSummaryHTML = '<h3>Resumo do Pedido:</h3>';
            pedidoData.itens.forEach(item => {
                const product = products.find(p => p.id === item.produtoId);
                if (product) {
                    orderSummaryHTML += `
                        <div class="cart-item">
                            <div>${product.name} x ${item.quantidade}</div>
                            <div>R$ ${(product.price * item.quantidade).toFixed(2)}</div>
                        </div>
                    `;
                }
            });
            
            orderSummaryHTML += `<div class="cart-total">Total: R$ ${total.toFixed(2)}</div>`;
            
            localStorage.setItem('lastOrderCode', orderCode);
            localStorage.setItem('lastOrderSummary', orderSummaryHTML);
            
            setTimeout(() => {
                window.location.href = 'confirmacao.html';
            }, 1500);
        } else {
            showAlert('Erro ao processar pedido: ' + data.message, 'error');
        }

    } catch (error) {
        console.error('Erro ao processar pedido:', error);
        showAlert('Erro ao conectar com o servidor: ' + error.message, 'error');
    }
}

function generateOrderCode() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let code = '';
    for (let i = 0; i < 9; i++) {
        code += chars.charAt(Math.floor(Math.random() * chars.length));
        if ((i + 1) % 3 === 0 && i < 8) code += '-';
    }
    return code;
}

// ========== FUNÇÕES DO VENDEDOR ==========
function loadVendedorProducts() {
    const vendedorProductsGrid = document.getElementById('vendedor-products-grid');
    if (!vendedorProductsGrid) return;
    
    vendedorProductsGrid.innerHTML = '';
    
    if (products.length === 0) {
        vendedorProductsGrid.innerHTML = '<p>Nenhum produto cadastrado.</p>';
        return;
    }
    
    products.forEach(product => {
        const productCard = document.createElement('div');
        productCard.className = `product-card ${!product.ativo ? 'inactive' : ''}`;

        const imageElement = product.image
            ? `<img src="/uploads/${product.image}" alt="${product.name}" class="product-image">`
            : `<div class="product-image placeholder">${product.name.charAt(0)}</div>`;

        productCard.innerHTML = `
            ${imageElement}
            <div class="product-info">
                <div class="product-title">${product.name} ${!product.ativo ? '(Inativo)' : ''}</div>
                <div class="product-price">R$ ${product.price.toFixed(2)}</div>
                <div class="product-description">${product.description}</div>
                <button onclick="editProduct(${product.id})" class="btn btn-primary" style="width: 100%; margin-top: 5px;">Editar</button>
                <button onclick="deleteProduct(${product.id})" class="btn btn-secondary" style="width: 100%; margin-top: 5px;">${product.ativo ? 'Desativar' : 'Reativar'}</button>
            </div>
        `;
        vendedorProductsGrid.appendChild(productCard);
    });
}

function addProduct() {
    const name = document.getElementById('product-name').value.trim();
    const description = document.getElementById('product-description').value.trim();
    const price = parseFloat(document.getElementById('product-price').value);
    
    // Validações
    if (!name || name.length < 2) {
        showAlert('Nome do produto deve ter pelo menos 2 caracteres', 'error');
        return;
    }
    
    if (!description || description.length < 5) {
        showAlert('Descrição deve ter pelo menos 5 caracteres', 'error');
        return;
    }
    
    if (isNaN(price) || price <= 0) {
        showAlert('Preço deve ser um valor maior que zero', 'error');
        return;
    }
    
    if (price > 999.99) {
        showAlert('Preço máximo é R$ 999,99', 'error');
        return;
    }
    
    // Verificar se produto já existe
    if (products.some(p => p.name.toLowerCase() === name.toLowerCase())) {
        showAlert('Já existe um produto com este nome', 'error');
        return;
    }
    
    const newProduct = {
        id: products.length > 0 ? Math.max(...products.map(p => p.id)) + 1 : 1,
        name: name,
        description: description,
        price: price,
        image: ''
    };
    
    products.push(newProduct);
    localStorage.setItem('cantina-products', JSON.stringify(products));
    showAlert('Produto adicionado com sucesso!', 'success');
    setTimeout(() => {
        window.location.href = 'gerenciarProdutos.html';
    }, 1000);
}



function editProduct(productId) {
    // Redirecionar para página de edição com o ID do produto
    window.location.href = `editarProduto.html?id=${productId}`;
}

// Função auxiliar para mostrar erro
function mostrarErro(mensagem) {
    document.getElementById('loading-message').style.display = 'none';
    document.getElementById('error-message').style.display = 'block';
    document.getElementById('error-message').textContent = mensagem;
}

function deleteProduct(productId) {
    if (confirm('Tem certeza que deseja remover este produto?')) {
        products = products.filter(p => p.id !== productId);
        localStorage.setItem('cantina-products', JSON.stringify(products));
        loadVendedorProducts();
        showAlert('Produto removido com sucesso!', 'success');
    }
}


// ========== FUNÇÕES DE MODO NOTURNO ==========
function initDarkMode() {
    const isDarkMode = localStorage.getItem('darkMode') === 'true';
    
    if (isDarkMode) {
        document.body.classList.add('dark-mode');
        updateThemeIcon();
    }
}

function toggleDarkMode() {
    const body = document.body;
    body.classList.toggle('dark-mode');
    
    const isDarkMode = body.classList.contains('dark-mode');
    localStorage.setItem('darkMode', isDarkMode);
    
    updateThemeIcon();
}

function updateThemeIcon() {
    const themeToggle = document.querySelector('.theme-toggle');
    if (themeToggle) {
        themeToggle.innerHTML = document.body.classList.contains('dark-mode') ? '☀️' : '🌙';
    }
}

// ========== FUNÇÕES DE AUTENTICAÇÃO ==========
async function verificarLogin() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    
    if (!username || !password) {
        showMessage('Por favor, preencha todos os campos', 'error');
        return;
    }
    
    try {
        const response = await fetch('http://localhost:8080/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                usuario: username,
                senha: password
            })
        });
        
        if (!response.ok) {
            throw new Error(`Erro HTTP: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('Login bem-sucedido! Redirecionando...', 'success');
            setTimeout(() => {
                localStorage.setItem('vendedorLogado', 'true');
                localStorage.setItem('vendedorNome', data.vendedor.nome);
                
                // ✅ ADICIONAR ESTA LINHA: Verificar se é admin
                verificarEhAdmin();
                
                window.location.href = 'vendedor.html';
            }, 1500);
        } else {
            showMessage(data.message || 'Usuário ou senha incorretos', 'error');
        }
        
    } catch (error) {
        console.error('Erro ao fazer login:', error);
        showMessage('Erro de conexão: ' + error.message, 'error');
    }
}

function verificarAutenticacao() {
    const isLoggedIn = localStorage.getItem('vendedorLogado') === 'true';
    
    const protectedPages = [
        'vendedor.html',
        'gerenciarProdutos.html',
        'adicionarProdutos.html',
        'pedidos.html'
    ];
    
    const currentPage = window.location.pathname.split('/').pop();
    
    if (!isLoggedIn && protectedPages.includes(currentPage)) {
        window.location.href = 'loginVendedor.html';
        return false;
    }
    
    return isLoggedIn;
}

function fazerLogout() {
    localStorage.removeItem('vendedorLogado');
    localStorage.removeItem('vendedorNome');
    showAlert('Logout realizado com sucesso!', 'success');
    setTimeout(() => {
        window.location.href = 'index.html';
    }, 1000);
}

function carregarNomeVendedor() {
    const nomeVendedor = localStorage.getItem('vendedorNome');
    if (nomeVendedor) {
        const elemento = document.getElementById('vendedor-nome');
        if (elemento) {
            elemento.textContent = nomeVendedor;
        }
    }
}

// ========== FUNÇÕES AUXILIARES ==========
function showMessage(message, type) {
    const messageDiv = document.getElementById('login-message');
    if (!messageDiv) return;
    
    messageDiv.textContent = message;
    messageDiv.className = `message ${type}`;
    messageDiv.style.display = 'block';
    
    if (type === 'error') {
        setTimeout(() => {
            messageDiv.style.display = 'none';
        }, 5000);
    }
}

function showAlert(message, type = 'info') {
    // Remove alertas anteriores
    const existingAlerts = document.querySelectorAll('.custom-alert');
    existingAlerts.forEach(alert => alert.remove());
    
    const alert = document.createElement('div');
    alert.className = `custom-alert ${type}`;
    alert.innerHTML = `
        <span>${message}</span>
        <button onclick="this.parentElement.remove()">×</button>
    `;
    
    // Estilos para o alerta
    alert.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        border-radius: 5px;
        color: white;
        z-index: 10000;
        display: flex;
        align-items: center;
        gap: 10px;
        max-width: 400px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        animation: slideIn 0.3s ease-out;
    `;
    
    // Cores baseadas no tipo
    const colors = {
        success: '#4CAF50',
        error: '#f44336',
        warning: '#ff9800',
        info: '#2196F3'
    };
    
    alert.style.backgroundColor = colors[type] || colors.info;
    
    alert.querySelector('button').style.cssText = `
        background: none;
        border: none;
        color: white;
        font-size: 18px;
        cursor: pointer;
        padding: 0;
        width: 20px;
        height: 20px;
        display: flex;
        align-items: center;
        justify-content: center;
    `;
    
    document.body.appendChild(alert);
    
    // Auto-remove após 5 segundos para tipos de sucesso/info
    if (type === 'success' || type === 'info') {
        setTimeout(() => {
            if (alert.parentElement) {
                alert.remove();
            }
        }, 5000);
    }
}

// Adicionar CSS para animação
if (!document.querySelector('#alert-styles')) {
    const style = document.createElement('style');
    style.id = 'alert-styles';
    style.textContent = `
        @keyframes slideIn {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
    `;
    document.head.appendChild(style);
}

// ========== SISTEMA DE CADASTRO ==========
function abrirModalCadastro() {
    document.getElementById('modalCadastro').style.display = 'flex';
}

function fecharModalCadastro() {
    document.getElementById('modalCadastro').style.display = 'none';
    document.getElementById('formCadastro').reset();
    limparMensagensErro();
}

function aplicarMascaras() {
    const cpfInput = document.getElementById('cpf');
    const telefoneInput = document.getElementById('telefone');
    const recoveryCpfInput = document.getElementById('recovery-cpf');
    
    [cpfInput, recoveryCpfInput].forEach(input => {
        if (input) {
            input.addEventListener('input', function(e) {
                let value = e.target.value.replace(/\D/g, '');
                if (value.length > 11) value = value.slice(0, 11);
                
                if (value.length > 9) {
                    value = value.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
                } else if (value.length > 6) {
                    value = value.replace(/(\d{3})(\d{3})(\d+)/, '$1.$2.$3');
                } else if (value.length > 3) {
                    value = value.replace(/(\d{3})(\d+)/, '$1.$2');
                }
                e.target.value = value;
            });
        }
    });
    
    if (telefoneInput) {
        telefoneInput.addEventListener('input', function(e) {
            let value = e.target.value.replace(/\D/g, '');
            if (value.length > 11) value = value.slice(0, 11);
            
            if (value.length > 10) {
                value = value.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
            } else if (value.length > 6) {
                value = value.replace(/(\d{2})(\d{4})(\d+)/, '($1) $2-$3');
            } else if (value.length > 2) {
                value = value.replace(/(\d{2})(\d+)/, '($1) $2');
            }
            e.target.value = value;
        });
    }
}

function limparMensagensErro() {
    const errors = document.querySelectorAll('.error-message');
    errors.forEach(error => {
        error.style.display = 'none';
        error.textContent = '';
    });
}

function mostrarErro(campoId, mensagem) {
    const errorElement = document.getElementById(campoId + '-error');
    if (errorElement) {
        errorElement.textContent = mensagem;
        errorElement.style.display = 'block';
    }
}

function validarEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

function validarCPF(cpf) {
    cpf = cpf.replace(/\D/g, '');
    if (cpf.length !== 11) return false;
    
    // Verifica se todos os dígitos são iguais
    if (/^(\d)\1{10}$/.test(cpf)) return false;
    
    // Validação de CPF usando algoritmo
    let soma = 0;
    let resto;
    
    for (let i = 1; i <= 9; i++) {
        soma += parseInt(cpf.substring(i-1, i)) * (11 - i);
    }
    
    resto = (soma * 10) % 11;
    if ((resto === 10) || (resto === 11)) resto = 0;
    if (resto !== parseInt(cpf.substring(9, 10))) return false;
    
    soma = 0;
    for (let i = 1; i <= 10; i++) {
        soma += parseInt(cpf.substring(i-1, i)) * (12 - i);
    }
    
    resto = (soma * 10) % 11;
    if ((resto === 10) || (resto === 11)) resto = 0;
    if (resto !== parseInt(cpf.substring(10, 11))) return false;
    
    return true;
}

function validarSenha(senha) {
    if (senha.length < 6) return 'Senha deve ter pelo menos 6 caracteres';
    if (!/(?=.*[A-Z])/.test(senha)) return 'Senha deve conter pelo menos uma letra maiúscula';
    if (!/(?=.*\d)/.test(senha)) return 'Senha deve conter pelo menos um número';
    return null;
}

async function cadastrarVendedor(vendedorData) {
    try {
        showAlert('Processando cadastro...', 'info');
        
        const response = await fetch('http://localhost:8080/api/auth/cadastro', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(vendedorData)
        });
        
        if (!response.ok) {
            throw new Error(`Erro HTTP: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.success) {
            showAlert(data.message, 'success');
            fecharModalCadastro();
            
            // Preencher automaticamente o login
            const usernameInput = document.getElementById('username');
            if (usernameInput) {
                usernameInput.value = vendedorData.usuario;
            }
            
            const passwordInput = document.getElementById('password');
            if (passwordInput) {
                passwordInput.focus();
            }
        } else {
            showAlert('Erro no cadastro: ' + data.message, 'error');
        }
        
    } catch (error) {
        console.error('Erro ao cadastrar:', error);
        showAlert('Erro ao conectar com o servidor. Tente novamente.', 'error');
    }
}

function inicializarEventListeners() {
    // Configurar o evento de submit do formulário de cadastro
    const formCadastro = document.getElementById('formCadastro');
    if (formCadastro) {
        formCadastro.addEventListener('submit', function(e) {
            e.preventDefault();
            limparMensagensErro();
            
            let isValid = true;
            
            // Validação do nome
            const nome = document.getElementById('nome').value.trim();
            if (nome.length < 3) {
                mostrarErro('nome', 'Nome deve ter pelo menos 3 caracteres');
                isValid = false;
            }
            
            // Validação do usuário
            const usuario = document.getElementById('usuario').value.trim();
            if (usuario.length < 4) {
                mostrarErro('usuario', 'Usuário deve ter pelo menos 4 caracteres');
                isValid = false;
            } else if (!/^[a-zA-Z0-9_]+$/.test(usuario)) {
                mostrarErro('usuario', 'Usuário só pode conter letras, números e underscore');
                isValid = false;
            }
            
            // Validação do CPF
            const cpf = document.getElementById('cpf').value;
            if (!validarCPF(cpf)) {
                mostrarErro('cpf', 'CPF inválido');
                isValid = false;
            }
            
            // Validação do email
            const email = document.getElementById('email').value.trim();
            if (!validarEmail(email)) {
                mostrarErro('email', 'E-mail inválido');
                isValid = false;
            }
            
            // Validação da senha
            const senha = document.getElementById('senha').value;
            const senhaError = validarSenha(senha);
            if (senhaError) {
                mostrarErro('senha', senhaError);
                isValid = false;
            }
            
            // Validação da confirmação de senha
            const confirmarSenha = document.getElementById('confirmar-senha').value;
            if (senha !== confirmarSenha) {
                mostrarErro('confirmar-senha', 'As senhas não coincidem');
                isValid = false;
            }
            
            // Validação das perguntas de segurança
            const pergunta1 = document.getElementById('pergunta1').value;
            const resposta1 = document.getElementById('resposta1').value.trim();
            if (!pergunta1 || !resposta1) {
                mostrarErro('resposta1', 'Selecione uma pergunta e forneça uma resposta');
                isValid = false;
            }
            
            const pergunta2 = document.getElementById('pergunta2').value;
            const resposta2 = document.getElementById('resposta2').value.trim();
            if (!pergunta2 || !resposta2) {
                mostrarErro('resposta2', 'Selecione uma pergunta e forneça uma resposta');
                isValid = false;
            }
            
            // Verificar se as perguntas são diferentes
            if (pergunta1 === pergunta2) {
                mostrarErro('resposta2', 'Selecione perguntas diferentes');
                isValid = false;
            }
            
            if (isValid) {
                // Preparar dados para envio
                const vendedorData = {
                    nome: nome,
                    usuario: usuario,
                    cpf: cpf.replace(/\D/g, ''),
                    email: email,
                    telefone: document.getElementById('telefone').value.replace(/\D/g, ''),
                    senha: senha,
                    perguntasSeguranca: [
                        { pergunta: pergunta1, resposta: resposta1 },
                        { pergunta: pergunta2, resposta: resposta2 }
                    ]
                };
                
                // Enviar dados para o backend
                cadastrarVendedor(vendedorData);
            }
        });
    }
}

// ========== SISTEMA DE RECUPERAÇÃO DE SENHA ==========
let recoveryData = {
    usuario: '',
    email: '',
    cpf: '',
    respostas: {},
    token: '',
    vendedorId: ''
};

function abrirModalRecuperacao() {
    document.getElementById('modalRecuperacao').style.display = 'flex';
    document.getElementById('recovery-step-1').classList.add('active');
    document.querySelector('.step[data-step="1"]').classList.add('active');
    
    // Resetar dados
    recoveryData = { usuario: '', email: '', cpf: '', respostas: {}, token: '', vendedorId: '' };
    
    // Limpar formulários
    const recoveryForm = document.getElementById('recoveryForm');
    if (recoveryForm) recoveryForm.reset();
}

function fecharModalRecuperacao() {
    document.getElementById('modalRecuperacao').style.display = 'none';
    
    // Resetar todos os steps
    document.querySelectorAll('.step-content').forEach(step => {
        step.classList.remove('active');
    });
    document.querySelectorAll('.step').forEach(step => {
        step.classList.remove('active');
    });
    
    // Voltar para o primeiro step
    document.getElementById('recovery-step-1').classList.add('active');
    document.querySelector('.step[data-step="1"]').classList.add('active');
    
    recoveryData = { usuario: '', email: '', cpf: '', respostas: {}, token: '', vendedorId: '' };
}

function avancarParaPerguntas() {
    const usuario = document.getElementById('recovery-usuario').value.trim();
    const email = document.getElementById('recovery-email').value.trim();
    const cpf = document.getElementById('recovery-cpf').value.replace(/\D/g, '');
    
    // Validações
    if (!usuario || !email || !cpf) {
        showAlert('Por favor, preencha todos os campos.', 'error');
        return;
    }
    
    if (usuario.length < 4) {
        showAlert('Usuário deve ter pelo menos 4 caracteres', 'error');
        return;
    }
    
    if (!validarEmail(email)) {
        showAlert('Por favor, insira um e-mail válido.', 'error');
        return;
    }
    
    if (!validarCPF(document.getElementById('recovery-cpf').value)) {
        showAlert('Por favor, insira um CPF válido.', 'error');
        return;
    }
    
    verificarDadosRecuperacao(usuario, email, cpf);
}

async function verificarDadosRecuperacao(usuario, email, cpf) {
    try {
        showAlert('Verificando dados...', 'info');
        
        const response = await fetch('http://localhost:8080/api/auth/verificar-dados-recuperacao', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ usuario, email, cpf })
        });
        
        if (!response.ok) {
            throw new Error(`Erro HTTP: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.success) {
            recoveryData.usuario = usuario;
            recoveryData.email = email;
            recoveryData.cpf = cpf;
            recoveryData.vendedorId = data.vendedorId;
            recoveryData.perguntas = data.perguntas;
            
            // Preencher as perguntas
            document.getElementById('pergunta-seguranca-1').textContent = data.perguntas[0];
            document.getElementById('pergunta-seguranca-2').textContent = data.perguntas[1];
            
            // Avançar para o próximo passo
            document.getElementById('recovery-step-1').classList.remove('active');
            document.getElementById('recovery-step-2').classList.add('active');
            
            document.querySelector('.step[data-step="1"]').classList.remove('active');
            document.querySelector('.step[data-step="2"]').classList.add('active');
            
            showAlert('Dados verificados com sucesso!', 'success');
        } else {
            showAlert(data.message || 'Dados não encontrados. Verifique as informações e tente novamente.', 'error');
        }
    } catch (error) {
        console.error('Erro ao verificar dados:', error);
        showAlert('Erro ao conectar com o servidor. Tente novamente.', 'error');
    }
}

function verificarRespostas() {
    const resposta1 = document.getElementById('resposta-seguranca-1').value.trim();
    const resposta2 = document.getElementById('resposta-seguranca-2').value.trim();
    
    if (!resposta1 || !resposta2) {
        showAlert('Por favor, responda ambas as perguntas.', 'error');
        return;
    }
    
    // Preparar as respostas no formato esperado pelo backend
    const respostasMap = {
        [document.getElementById('pergunta-seguranca-1').textContent]: resposta1,
        [document.getElementById('pergunta-seguranca-2').textContent]: resposta2
    };
    
    recoveryData.respostas = respostasMap;
    validarRespostasSeguranca();
}

async function validarRespostasSeguranca() {
    try {
        showAlert('Verificando respostas...', 'info');
        
        const response = await fetch('http://localhost:8080/api/auth/verificar-respostas', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                usuario: recoveryData.usuario,
                respostas: recoveryData.respostas
            })
        });
        
        if (!response.ok) {
            throw new Error(`Erro HTTP: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.success) {
            recoveryData.token = data.token;
            recoveryData.vendedorId = data.vendedorId;
            
            // Avançar para o próximo passo
            document.getElementById('recovery-step-2').classList.remove('active');
            document.getElementById('recovery-step-3').classList.add('active');
            
            document.querySelector('.step[data-step="2"]').classList.remove('active');
            document.querySelector('.step[data-step="3"]').classList.add('active');
            
            showAlert('Respostas verificadas com sucesso!', 'success');
        } else {
            showAlert(data.message || 'Respostas incorretas. Tente novamente.', 'error');
        }
    } catch (error) {
        console.error('Erro ao verificar respostas:', error);
        showAlert('Erro ao conectar com o servidor. Tente novamente.', 'error');
    }
}

function redefinirSenha() {
    const novaSenha = document.getElementById('nova-senha').value;
    const confirmarSenha = document.getElementById('confirmar-nova-senha').value;
    
    if (!novaSenha || !confirmarSenha) {
        showAlert('Por favor, preencha ambos os campos de senha.', 'error');
        return;
    }
    
    const senhaError = validarSenha(novaSenha);
    if (senhaError) {
        showAlert(senhaError, 'error');
        return;
    }
    
    if (novaSenha !== confirmarSenha) {
        showAlert('As senhas não coincidem.', 'error');
        return;
    }
    
    enviarNovaSenha(novaSenha);
}

async function enviarNovaSenha(novaSenha) {
    try {
        showAlert('Redefinindo senha...', 'info');
        
        const response = await fetch('http://localhost:8080/api/auth/redefinir-senha', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                usuario: recoveryData.usuario,
                novaSenha: novaSenha
            })
        });
        
        if (!response.ok) {
            throw new Error(`Erro HTTP: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.success) {
            showAlert('Senha redefinida com sucesso!', 'success');
            fecharModalRecuperacao();
            
            // Preencher automaticamente o login
            const usernameInput = document.getElementById('username');
            const passwordInput = document.getElementById('password');
            
            if (usernameInput) usernameInput.value = recoveryData.usuario;
            if (passwordInput) passwordInput.focus();
        } else {
            showAlert('Erro ao redefinir a senha: ' + data.message, 'error');
        }
        
    } catch (error) {
        console.error('Erro ao redefinir senha:', error);
        showAlert('Erro ao conectar com o servidor. Tente novamente.', 'error');
    }
}

function voltarStep(stepAtual) {
    const stepAnterior = stepAtual - 1;
    
    document.getElementById(`recovery-step-${stepAtual}`).classList.remove('active');
    document.getElementById(`recovery-step-${stepAnterior}`).classList.add('active');
    
    document.querySelector(`.step[data-step="${stepAtual}"]`).classList.remove('active');
    document.querySelector(`.step[data-step="${stepAnterior}"]`).classList.add('active');
}

// ========== INICIALIZAÇÃO PARA PÁGINAS DO VENDEDOR ==========
function initVendedorPages() {
    if (!verificarAutenticacao()) {
        showAlert('Você precisa fazer login para acessar esta área.', 'error');
    } else {
        carregarNomeVendedor();
        loadVendedorProducts();
    }
}

// Inicialização quando a página carrega
document.addEventListener('DOMContentLoaded', function() {
    // Verificar autenticação para páginas protegidas
    const protectedPages = ['vendedor.html', 'gerenciarProdutos.html', 'adicionarProdutos.html', 'pedidos.html'];
    const currentPage = window.location.pathname.split('/').pop();
    
    if (protectedPages.includes(currentPage) && !verificarAutenticacao()) {
        showAlert('Você precisa fazer login para acessar esta área.', 'error');
    } else if (protectedPages.includes(currentPage)) {
        carregarNomeVendedor();
        // Os produtos serão carregados automaticamente pelo carregarProdutosDoBanco()
    }
    
    // Carregar produtos na página de produtos
    if (currentPage === 'produtos.html') {
        // VERIFICAR CONFLITO DE SESSÃO PRIMEIRO
        if (limparDadosSessaoAnterior()) {
            return; // Para a execução se houver conflito
        }
        
        // Os produtos serão carregados automaticamente pelo carregarProdutosDoBanco()
        
        // Mostrar nome do cliente
        const customerNameDisplay = document.getElementById('customer-name-display');
        const savedCustomerName = localStorage.getItem('customerName');
        
        if (customerNameDisplay && savedCustomerName) {
            customerNameDisplay.textContent = savedCustomerName;
            // GARANTIR que o nome está correto
            customerName = savedCustomerName;
        } else if (customerNameDisplay && !savedCustomerName) {
            window.location.href = 'identificacaoCliente.html';
        }
    }
    
    // Atualizar carrinho se estiver na página do carrinho
    if (currentPage === 'carrinho.html') {
        // VERIFICAR CONFLITO DE SESSÃO
        if (limparDadosSessaoAnterior()) {
            return;
        }
        
        updateCart();
    }
    
    // Carregar pedidos se estiver na página de pedidos do vendedor
    if (currentPage === 'pedidos.html') {
        loadVendedorOrders();
    }
});


    async function carregarProdutosDoBanco() {
        try {
            const response = await fetch('http://localhost:8080/api/produtos');
            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }
            
            const produtosDoBanco = await response.json();
            
            // Converter para o formato esperado pelo sistema
            products = produtosDoBanco.map(produto => ({
                id: produto.id,
                name: produto.nome,
                description: produto.descricao,
                price: parseFloat(produto.preco),
                image: produto.imagem || '',
                estoque: produto.estoque || 0,
                ativo: produto.ativo
            }));
            
            // Atualizar a interface
            if (window.location.pathname.includes('produtos.html')) {
                loadProducts();
            }
            if (window.location.pathname.includes('vendedor.html') || 
                window.location.pathname.includes('gerenciarProdutos.html')) {
                loadVendedorProducts();
            }
            
        } catch (error) {
            console.error('Erro ao carregar produtos do banco:', error);
            // Usar produtos locais como fallback
            showAlert('Usando dados locais - Servidor offline', 'warning');
        }
    }
    
    // Adicionar produto integrado com banco
    async function addProduct() {
        const name = document.getElementById('product-name').value.trim();
        const description = document.getElementById('product-description').value.trim();
        const price = parseFloat(document.getElementById('product-price').value);
        const estoque = parseInt(document.getElementById('product-estoque').value) || 0;
        const imagemInput = document.getElementById('product-image');
        const imagem = imagemInput.files[0];

        // Validações
        if (!name || name.length < 2) {
            showAlert('Nome do produto deve ter pelo menos 2 caracteres', 'error');
            return;
        }
        if (!description || description.length < 5) {
            showAlert('Descrição deve ter pelo menos 5 caracteres', 'error');
            return;
        }
        if (isNaN(price) || price <= 0) {
            showAlert('Preço deve ser um valor maior que zero', 'error');
            return;
        }
        if (price > 999.99) {
            showAlert('Preço máximo é R$ 999,99', 'error');
            return;
        }

        try {
            showAlert('Salvando produto...', 'info');

            const formData = new FormData();
            formData.append('nome', name);
            formData.append('descricao', description);
            formData.append('preco', price);
            formData.append('estoque', estoque);
            if (imagem) {
                formData.append('imagem', imagem);
            }

            const response = await fetch('http://localhost:8080/api/produtos', {
                method: 'POST',
                body: formData
                // NÃO definir 'Content-Type', o navegador fará isso automaticamente para multipart/form-data
            });

            if (!response.ok) {
                 const errorData = await response.json().catch(() => ({ message: 'Erro desconhecido no servidor.' }));
                 throw new Error(errorData.message || `Erro HTTP: ${response.status}`);
            }

            const data = await response.json();

            if (data.success) {
                showAlert('Produto adicionado com sucesso!', 'success');
                await carregarProdutosDoBanco();
                setTimeout(() => {
                    window.location.href = 'gerenciarProdutos.html';
                }, 1000);
            } else {
                showAlert('Erro ao adicionar produto: ' + data.message, 'error');
            }

        } catch (error) {
            console.error('Erro ao adicionar produto:', error);
            showAlert('Erro ao conectar com o servidor: ' + error.message, 'error');
        }
    }
    
    // Deletar produto integrado com banco
    async function deleteProduct(productId) {
        if (!confirm('Tem certeza que deseja remover este produto?')) {
            return;
        }
    
        try {
            showAlert('Removendo produto do banco de dados...', 'info');
    
            const response = await fetch(`http://localhost:8080/api/produtos/${productId}`, {
                method: 'DELETE'
            });
    
            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }
    
            const data = await response.json();
    
            if (data.success) {
                showAlert('Produto removido com sucesso do banco de dados!', 'success');
                // Recarregar produtos do banco
                await carregarProdutosDoBanco();
            } else {
                showAlert('Erro ao remover produto: ' + data.message, 'error');
            }
    
        } catch (error) {
            console.error('Erro ao remover produto:', error);
            showAlert('Erro ao conectar com o servidor. Removendo localmente.', 'warning');
            
            // Fallback para localStorage
            products = products.filter(p => p.id !== productId);
            localStorage.setItem('cantina-products', JSON.stringify(products));
            loadVendedorProducts();
            showAlert('Produto removido localmente!', 'success');
        }
    }






















    async function processPayment() {
        if (cart.length === 0) {
            showAlert('Seu carrinho está vazio!', 'error');
            return;
        }
        
        const total = cart.reduce((sum, item) => sum + (item.product.price * item.quantity), 0);
        if (total < 5.00) {
            showAlert('Valor mínimo do pedido é R$ 5,00', 'error');
            return;
        }
    
        try {
            showAlert('Processando pedido...', 'info');
    
            const pedidoData = {
                clienteNome: customerName,
                metodoPagamento: 'PIX', // Mudar para PIX
                itens: cart.map(item => ({
                    produtoId: item.product.id,
                    quantidade: item.quantity
                }))
            };
    
            const response = await fetch('http://localhost:8080/api/pedidos', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(pedidoData)
            });
    
            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }
    
            const data = await response.json();
    
            if (data.success) {
                // 🎯 REDIRECIONAR PARA PAGAMENTO PIX
                const pedidoId = data.pedido.id;
                localStorage.setItem('ultimoPedidoId', pedidoId);
                
                showAlert('Pedido criado! Redirecionando para pagamento...', 'success');
                
                setTimeout(() => {
                    window.location.href = `pagamentoPix.html?pedidoId=${pedidoId}`;
                }, 1500);
                
            } else {
                showAlert('Erro ao processar pedido: ' + data.message, 'error');
            }
    
        } catch (error) {
            console.error('Erro ao processar pedido:', error);
            showAlert('Erro ao conectar com o servidor: ' + error.message, 'error');
        }
    }





























    
    async function carregarPedidosDoBanco() {
        try {
            console.log('Tentando carregar pedidos...');
            
            // Primeiro testar a conexão
            const testeResponse = await fetch('http://localhost:8080/api/pedidos/teste');
            console.log('Teste status:', testeResponse.status);
            
            if (testeResponse.ok) {
                const testeData = await testeResponse.json();
                console.log('Teste resposta:', testeData);
            }
            
            // Agora tentar carregar os pedidos
            const response = await fetch('http://localhost:8080/api/pedidos');
            console.log('Pedidos status:', response.status);
            
            if (!response.ok) {
                const errorText = await response.text();
                console.error('Erro detalhado:', errorText);
                throw new Error(`Erro HTTP: ${response.status} - ${errorText}`);
            }
            
            const pedidosDoBanco = await response.json();
            console.log('Pedidos carregados:', pedidosDoBanco);
            return pedidosDoBanco;
            
        } catch (error) {
            console.error('Erro completo ao carregar pedidos:', error);
            showAlert('Erro ao carregar pedidos: ' + error.message, 'error');
            return [];
        }
    }

    async function atualizarStatusPedido(pedidoId, novoStatus) {
        try {
            console.log(`Atualizando pedido ${pedidoId} para status: ${novoStatus}`);
            showAlert('Atualizando status do pedido...', 'info');
            
            const response = await fetch(`http://localhost:8080/api/pedidos/${pedidoId}/status`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    status: novoStatus
                })
            });
    
            console.log('Resposta do servidor - Status:', response.status);
            
            // Verificar se a resposta é OK (status 200-299)
            if (response.ok) {
                try {
                    const data = await response.json();
                    console.log('Dados retornados:', data);
                    
                    if (data.success) {
                        showAlert('Status do pedido atualizado com sucesso!', 'success');
                        
                        // Aguardar 1 segundo e recarregar a página
                        setTimeout(() => {
                            location.reload();
                        }, 1000);
                        
                    } else {
                        // O servidor respondeu mas com success=false
                        showAlert('Erro: ' + (data.message || 'Status não atualizado'), 'error');
                    }
                    
                } catch (jsonError) {
                    console.error('Erro ao parsear JSON:', jsonError);
                    // Mesmo com erro no JSON, se o status HTTP é OK, provavelmente funcionou
                    showAlert('Status atualizado! Recarregando...', 'success');
                    setTimeout(() => {
                        location.reload();
                    }, 1000);
                }
                
            } else {
                // Resposta não é OK (erro HTTP)
                const errorText = await response.text();
                console.error('Erro HTTP:', response.status, errorText);
                throw new Error(`Erro ${response.status}: ${errorText}`);
            }
            
        } catch (error) {
            console.error('Erro completo:', error);
            showAlert('Erro ao conectar com o servidor: ' + error.message, 'error');
        }
    }

async function carregarPedidosVendedor() {
    const ordersList = document.getElementById('orders-list');
    if (!ordersList) return;
    
    try {
        ordersList.innerHTML = '<div class="loading">Carregando pedidos...</div>';
        
        const pedidos = await carregarPedidosDoBanco();
        
        ordersList.innerHTML = '';
        
        if (pedidos.length === 0) {
            ordersList.innerHTML = '<p>Nenhum pedido recebido ainda.</p>';
            return;
        }
        
        // Ordenar pedidos: pendentes primeiro, depois por data (mais recente primeiro)
        pedidos.sort((a, b) => {
            if (a.status !== b.status) {
                // Pendentes primeiro
                if (a.status === 'PENDENTE') return -1;
                if (b.status === 'PENDENTE') return 1;
            }
            // Depois por data (mais recente primeiro)
            return new Date(b.dataPedido) - new Date(a.dataPedido);
        });
        
        pedidos.forEach(pedido => {
            const orderItem = document.createElement('div');
            orderItem.className = 'order-item';
            orderItem.id = `pedido-${pedido.id}`;
            
            // Formatar data
            const dataPedido = new Date(pedido.dataPedido).toLocaleString('pt-BR');
            
            // Calcular total dos itens
            let itemsHtml = '';
            if (pedido.itens && pedido.itens.length > 0) {
                pedido.itens.forEach(item => {
                    const itemTotal = item.precoUnitario * item.quantidade;
                    itemsHtml += `
                        <div class="order-item-line">
                            <span class="item-quantity">${item.quantidade}x</span>
                            <span class="item-name">${item.produto.nome}</span>
                            <span class="item-price">R$ ${itemTotal.toFixed(2)}</span>
                        </div>
                    `;
                });
            }
            
            // Definir cor do status
            const statusColors = {
                'PENDENTE': 'status-pending',
                'PREPARANDO': 'status-preparing',
                'PRONTO': 'status-ready',
                'ENTREGUE': 'status-completed',
                'CANCELADO': 'status-cancelled'
            };
            
            const statusTexts = {
                'PENDENTE': 'Pendente',
                'PREPARANDO': 'Preparando',
                'PRONTO': 'Pronto para Retirada',
                'ENTREGUE': 'Entregue',
                'CANCELADO': 'Cancelado'
            };
            
            orderItem.innerHTML = `
                <div class="order-header">
                    <div>
                        <div class="order-customer"><strong>${pedido.clienteNome}</strong></div>
                        <div class="order-code">Pedido: ${pedido.codigo}</div>
                        <div class="order-date">${dataPedido}</div>
                        <div class="order-method">Pagamento: ${pedido.metodoPagamento || 'Não informado'}</div>
                    </div>
                    <div class="order-status ${statusColors[pedido.status]}">
                        ${statusTexts[pedido.status]}
                    </div>
                </div>
                
                <div class="order-items">
                    ${itemsHtml}
                </div>
                
                <div class="order-footer">
                    <div class="order-total">Total: R$ ${pedido.total.toFixed(2)}</div>
                    
                    <div class="order-actions">
                        ${pedido.status === 'PENDENTE' ? 
                            `<button class="btn btn-primary" onclick="atualizarStatusPedido(${pedido.id}, 'PREPARANDO')">
                                Iniciar Preparo
                            </button>` : ''}
                        
                        ${pedido.status === 'PREPARANDO' ? 
                            `<button class="btn btn-success" onclick="atualizarStatusPedido(${pedido.id}, 'PRONTO')">
                                Marcar como Pronto
                            </button>` : ''}
                        
                        ${pedido.status === 'PRONTO' ? 
                            `<button class="btn btn-completed" onclick="atualizarStatusPedido(${pedido.id}, 'ENTREGUE')">
                                Marcar como Entregue
                            </button>` : ''}
                        
                        ${pedido.status !== 'ENTREGUE' && pedido.status !== 'CANCELADO' ? 
                            `<button class="btn btn-secondary" onclick="atualizarStatusPedido(${pedido.id}, 'CANCELADO')">
                                Cancelar Pedido
                            </button>` : ''}
                    </div>
                </div>
            `;
            
            ordersList.appendChild(orderItem);
        });
        
    } catch (error) {
        console.error('Erro ao carregar pedidos:', error);
        ordersList.innerHTML = '<p class="error">Erro ao carregar pedidos. Verifique o servidor.</p>';
    }
}

// Adicionar estilos CSS para os novos status
function adicionarEstilosPedidos() {
    if (!document.querySelector('#pedidos-styles')) {
        const style = document.createElement('style');
        style.id = 'pedidos-styles';
        style.textContent = `
            .order-item-line {
                display: flex;
                justify-content: space-between;
                margin-bottom: 5px;
                padding: 5px 0;
                border-bottom: 1px solid #eee;
            }
            
            .item-quantity {
                font-weight: bold;
                min-width: 30px;
            }
            
            .item-name {
                flex: 1;
                margin: 0 10px;
            }
            
            .item-price {
                font-weight: bold;
                color: var(--primary-color);
            }
            
            .order-footer {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-top: 15px;
                padding-top: 15px;
                border-top: 2px solid #eee;
            }
            
            .order-total {
                font-weight: bold;
                font-size: 18px;
                color: var(--dark-color);
            }
            
            .order-actions {
                display: flex;
                gap: 10px;
                flex-wrap: wrap;
            }
            
            .btn-success {
                background-color: var(--success-color);
                color: white;
            }
            
            .btn-success:hover {
                background-color: #45a049;
            }
            
            .btn-completed {
                background-color: #2196F3;
                color: white;
            }
            
            .btn-completed:hover {
                background-color: #1976D2;
            }
            
            .status-ready {
                background-color: #d1ecf1;
                color: #0c5460;
            }
            
            .status-cancelled {
                background-color: #f8d7da;
                color: #721c24;
            }
            
            .order-customer {
                font-size: 18px;
                margin-bottom: 5px;
            }
            
            .order-method {
                font-size: 14px;
                color: #666;
                margin-top: 2px;
            }
            
            .loading {
                text-align: center;
                padding: 20px;
                color: #666;
            }
            
            .error {
                color: #f44336;
                text-align: center;
                padding: 20px;
            }
            
            body.dark-mode .order-item-line {
                border-bottom-color: #444;
            }
            
            body.dark-mode .order-footer {
                border-top-color: #444;
            }
            
            body.dark-mode .order-total {
                color: #f0f0f0;
            }
            
            body.dark-mode .order-method {
                color: #bbb;
            }
        `;
        document.head.appendChild(style);
    }
}

function limparDadosSessaoAnterior() {
    // Verificar se o nome do cliente mudou
    const customerName = localStorage.getItem('customerName');
    const currentCustomerName = document.getElementById('customer-name-display')?.textContent;
    
    // Se detectar conflito de nomes, limpar tudo
    if (currentCustomerName && customerName && currentCustomerName !== customerName) {
        console.log('Conflito de sessão detectado! Limpando dados...');
        localStorage.removeItem('cart');
        localStorage.removeItem('customerName');
        localStorage.removeItem('lastOrderCode');
        localStorage.removeItem('lastOrderSummary');
        
        // Recarregar a página para aplicar as mudanças
        setTimeout(() => {
            window.location.href = 'identificacaoCliente.html';
        }, 100);
        return true;
    }
    return false;
}

// NO app.js - ADICIONAR ESTAS FUNÇÕES:

// Função para limpar carrinho no banco
async function limparCarrinhoBanco(sessaoId) {
    try {
        const response = await fetch(`http://localhost:8080/api/sessao/carrinho/${sessaoId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            console.log('✅ Carrinho limpo no banco');
            return true;
        }
        return false;
    } catch (error) {
        console.error('Erro ao limpar carrinho no banco:', error);
        return false;
    }
}

// Função para finalizar sessão (usar quando pedido for concluído)
async function finalizarSessao(sessaoId) {
    try {
        const response = await fetch(`http://localhost:8080/api/sessao/finalizar/${sessaoId}`, {
            method: 'POST'
        });
        
        if (response.ok) {
            console.log('✅ Sessão finalizada');
            // Remover do localStorage também
            localStorage.removeItem('sessaoId');
            localStorage.removeItem('customerName');
            localStorage.removeItem('cart');
            return true;
        }
        return false;
    } catch (error) {
        console.error('Erro ao finalizar sessão:', error);
        return false;
    }
}

// Limpeza automática ao voltar para o início
function limparAovoltarInicio() {
    const sessaoId = localStorage.getItem('sessaoId');
    
    if (sessaoId) {
        console.log('🧹 Limpando dados ao voltar para início...');
        
        // Tentar limpar no banco (não bloqueante)
        limparCarrinhoBanco(sessaoId).then(success => {
            if (success) {
                console.log('✅ Carrinho limpo no banco');
            }
        });
        
        // Limpar localStorage
        localStorage.removeItem('sessaoId');
        localStorage.removeItem('cart');
        // Não remove customerName para manter o nome se voltar
    }
    
    // Redirecionar para index
    window.location.href = 'index.html';
}

if (window.location.pathname.includes('identificacaoCliente.html')) {
    const sessaoId = localStorage.getItem('sessaoId');
    if (sessaoId) {
        console.log('🔄 Nova identificação - limpando sessão anterior...');
        limparCarrinhoBanco(sessaoId);
        localStorage.removeItem('sessaoId');
        localStorage.removeItem('cart');
    }
}

function verificarEhAdmin() {
    const usuario = localStorage.getItem('vendedorNome');
    const isAdmin = usuario === 'admin' || usuario === 'Administrador';
    
    const adminLink = document.getElementById('admin-link');
    if (adminLink) {
        adminLink.style.display = isAdmin ? 'block' : 'none';
    }
}

function verificarEhAdmin() {
    const usuario = localStorage.getItem('vendedorNome');
    const isAdmin = usuario === 'admin' || usuario === 'Administrador';
    
    console.log('🔍 Verificando admin:', usuario, 'É admin?', isAdmin);
    
    const adminLink = document.getElementById('admin-link');
    if (adminLink) {
        adminLink.style.display = isAdmin ? 'block' : 'none';
        console.log('🔗 Link admin:', adminLink.style.display);
    }
}


// 🆕 FUNÇÃO PARA CARREGAR APENAS PEDIDOS ATIVOS
async function carregarPedidosAtivosDoBanco() {
    try {
        const response = await fetch('http://localhost:8080/api/pedidos/ativos');
        
        if (!response.ok) {
            throw new Error(`Erro HTTP: ${response.status}`);
        }
        
        const pedidos = await response.json();
        return pedidos;
        
    } catch (error) {
        console.error('Erro ao carregar pedidos ativos:', error);
        throw error;
    }
}

// 🆕 FUNÇÃO PARA VER TODOS OS PEDIDOS (incluindo entregues)
async function carregarTodosPedidosDoBanco() {
    try {
        const response = await fetch('http://localhost:8080/api/pedidos');
        
        if (!response.ok) {
            throw new Error(`Erro HTTP: ${response.status}`);
        }
        
        const pedidos = await response.json();
        return pedidos;
        
    } catch (error) {
        console.error('Erro ao carregar todos os pedidos:', error);
        throw error;
    }
}