#!/bin/bash
export PATH=$PATH:/C/dev/Java/jdk1.8.0_111/bin
# generate client and server key stores (used for testing)

# clean up
rm -fr private newcerts certs serial* index.txt* *.pem *.jks

# create ca
mkdir private certs newcerts
ls -R
echo '01' > serial
touch index.txt
openssl req -new -x509 -passout pass:passphrase -extensions v3_ca -keyout private/cakey.pem -out certs/cacert.pem -days 7305 -config caconfig.conf -subj "//x=x/C=US/ST=NV/L=Area 51/O=Hangar 18/OU=X.500 Standard Developers/CN=Test Root CA"

keytool -importcert -alias ca -file certs/cacert.pem -keypass passphrase -noprompt -trustcacerts -storetype JKS -keystore trust.jks -storepass passphrase
keytool -list -v -keystore trust.jks -storepass passphrase


# create server
keytool -genkeypair -alias localhost -keyalg RSA -keysize 2048 -sigalg SHA512withRSA -dname "C=US, ST=NV, L=Area 51, O=Hangar 18, OU=X.500 Standard Developers, CN=Localhost Server" -keypass passphrase -validity 7305 -storetype JKS -keystore server.jks -storepass passphrase
keytool -certreq -alias localhost -sigalg SHA512withRSA -file certs/localhost.req.pem -keypass passphrase -storetype JKS -keystore server.jks -storepass passphrase
openssl ca -batch -passin pass:passphrase -notext -out certs/localhost.pem -extensions server -config caconfig.conf -days 7305 -infiles certs/localhost.req.pem 
echo !!!!!!!!!!!!!!!!!!!!! server BEFORE
keytool -list -v -keystore server.jks -storepass passphrase
keytool -importcert -alias ca -file certs/cacert.pem -keypass passphrase -noprompt -trustcacerts -storetype JKS -keystore server.jks -storepass passphrase
keytool -importcert -alias localhost -file certs/localhost.pem -keypass passphrase -noprompt -storetype JKS -keystore server.jks -storepass passphrase
echo !!!!!!!!!!!!!!!!!!!!! server after
keytool -list -v -keystore server.jks -storepass passphrase

# create client
keytool -genkeypair -alias client -keyalg RSA -keysize 2048 -sigalg SHA512withRSA -dname "C=US, ST=NV, L=Area 51, O=Hangar 18, OU=X.500 Standard Developers, CN=Local Client" -keypass passphrase -validity 7305 -storetype JKS -keystore client.jks -storepass passphrase
keytool -certreq -alias client -sigalg SHA512withRSA -file certs/localclient.req.pem -keypass passphrase -storetype JKS -keystore client.jks -storepass passphrase
openssl ca -batch -passin pass:passphrase -notext -out certs/localclient.pem -days 7305 -config caconfig.conf -extensions client -infiles certs/localclient.req.pem 
echo !!!!!!!!!!!!!!!!!!!!! cliENT BEFORE
keytool -list -v -keystore client.jks -storepass passphrase
keytool -importcert -alias ca -file certs/cacert.pem -keypass passphrase -noprompt -trustcacerts -storetype JKS -keystore client.jks -storepass passphrase
keytool -importcert -alias client -file certs/localclient.pem -keypass passphrase -noprompt -storetype JKS -keystore client.jks -storepass passphrase
echo !!!!!!!!!!!!!!!!!!!!! cliENT aFTER
keytool -list -v -keystore client.jks -storepass passphrase

# post process cleanup
rm -fr private newcerts certs serial* index.txt* *.pem
