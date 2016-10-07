package com.mservice.momo.gateway.internal.visamaster;

import com.mservice.visa.entity.CardType;

/**
 * Created by khoanguyen on 18/03/2015.
 */
public class VMCardType {

    public static final String VISA = "001";//	Visa
    public static final String MASTERCARD = "002";//	MasterCard
    public static final String AMERICAN_EXPRESS = "003";//	American Express
    public static final String DISCOVER = "004";//	Discover
    public static final String DINERS_CLUB = "005";//	Diners Club
    public static final String CARTE_BLANCHE = "006";//	Carte Blanche.
    public static final String JCB = "007";//	JCB
    public static final String ENROUTE = "014";//	EnRoute
    public static final String JAL = "021";//	JAL
    public static final String DELTA = "031";//	Delta
    public static final String VISA_ELECTRON = "033";//	Visa Electron
    public static final String DANKORT = "034";//	Dankort
    public static final String CARTE_BLEUE = "036";//	Carte Bleue
    public static final String CARTA_SI = "037";//	Carta Si
    public static final String MAESTRO_UK_DOMESTIC = "024";//	Maestro UK Domestic
    public static final String MAESTRO_INTERNATIONAL = "042";//	Maestro International
    public static final String GE_MONEY_UK_CARD = "043";//	GE Money UK card

    public static final String VISA_STRING = "Visa";//	Visa
    public static final String MASTERCARD_STRING = "MasterCard";//	MasterCard
    public static final String AMERICAN_EXPRESS_STRING = "American Express";//	American Express
    public static final String DISCOVER_STRING = "Discover";//	Discover
    public static final String DINERS_CLUB_STRING = "Diners Club";//	Diners Club
    public static final String CARTE_BLANCHE_STRING = "Carte Blanche";//	Carte Blanche.
    public static final String JCB_STRING = "JCB";//	JCB
    public static final String ENROUTE_STRING = "EnRoute";//	EnRoute
    public static final String JAL_STRING = "JAL";//	JAL
    public static final String DELTA_STRING = "Delta";//	Delta
    public static final String VISA_ELECTRON_STRING = "Visa Electron";//	Visa Electron
    public static final String DANKORT_STRING = "Dankort";//	Dankort
    public static final String CARTE_BLEUE_STRING = "Carte Bleue";//	Carte Bleue
    public static final String CARTA_SI_STRING = "Carta Si";//	Carta Si
    public static final String MAESTRO_UK_DOMESTIC_STRING = "Maestro UK Domestic";//	Maestro UK Domestic
    public static final String MAESTRO_INTERNATIONAL_STRING = "Maestro International";//	Maestro International
    public static final String GE_MONEY_UK_CARD_STRING = "GE Money UK";//	GE Money UK card


    private String userName = "";
    private String partnerCode = "";

    private String cardTypeId = "";
    private long ltime = 0;
    private String desc = "";
    private boolean isEnable = false;



    public static CardType getCardType(String codeCardType)
    {
        CardType cardType = null;

        switch (codeCardType)
        {
            case(VISA):
                cardType = CardType.VISA;
                break;
            case(MASTERCARD):
                cardType = CardType.MASTER_CARD;
                break;
            case(AMERICAN_EXPRESS):
                cardType = CardType.AMERICAN_EXPRESS;
                break;
            case(DISCOVER):
                cardType = CardType.DISCOVER;
                break;
            case(DINERS_CLUB):
                cardType = CardType.DINERS_CLUB;
                break;
            case(CARTE_BLANCHE):
                cardType = CardType.CARTE_BLANCHE;
                break;
            case(JCB):
                cardType = CardType.JCB;
                break;
            case(ENROUTE):
                cardType = CardType.ENROUTE;
                break;
            case(JAL):
                cardType = CardType.JAL;
                break;
            case(DELTA):
                cardType = CardType.DELTA;
                break;
            case(VISA_ELECTRON):
                cardType = CardType.VISA_ELECTRON;
                break;
            case(DANKORT):
                cardType = CardType.DANKORT;
                break;
            case(CARTE_BLEUE):
                cardType = CardType.CARTE_BLEUE;
                break;
            case(CARTA_SI):
                cardType = CardType.CARTA_SI;
                break;
            case(MAESTRO_UK_DOMESTIC):
                cardType = CardType.MAESTRO_UK_DOMESTIC;
                break;
            case(MAESTRO_INTERNATIONAL):
                cardType = CardType.MAESTRO_INTERNATIONAL;
                break;
            case(GE_MONEY_UK_CARD):
                cardType = CardType.GE_MONEY_UK_CARD;
                break;
            default:
                cardType = null;
                break;
        }

        return cardType;
    }


    public static String getCodeCardType(CardType cardType) {
        String codeCardType = "";


        if (cardType.equals(CardType.VISA)) {
            codeCardType = VISA;
        } else if (cardType.equals(CardType.MASTER_CARD)) {
            codeCardType = MASTERCARD;
        } else if (cardType.equals(CardType.AMERICAN_EXPRESS)) {
            codeCardType = AMERICAN_EXPRESS;
        } else if (cardType.equals(CardType.DISCOVER)) {
            codeCardType = DISCOVER;
        } else if (cardType.equals(CardType.DINERS_CLUB)) {
            codeCardType = DINERS_CLUB;
        } else if (cardType.equals(CardType.CARTE_BLANCHE)) {
            codeCardType = CARTE_BLANCHE;
        } else if (cardType.equals(CardType.JCB)) {
            codeCardType = JCB;
        } else if (cardType.equals(CardType.ENROUTE)) {
            codeCardType = ENROUTE;
        } else if (cardType.equals(CardType.JAL)) {
            codeCardType = JAL;
        } else if (cardType.equals(CardType.DELTA)) {
            codeCardType = DELTA;
        } else if (cardType.equals(CardType.VISA_ELECTRON)) {
            codeCardType = VISA_ELECTRON;
        } else if (cardType.equals(CardType.DANKORT)) {
            codeCardType = DANKORT;
        } else if (cardType.equals(CardType.CARTE_BLEUE)) {
            codeCardType = CARTE_BLEUE;
        } else if (cardType.equals(CardType.CARTA_SI)) {
            codeCardType = CARTA_SI;
        } else if (cardType.equals(CardType.MAESTRO_UK_DOMESTIC)) {
            codeCardType = MAESTRO_UK_DOMESTIC;
        } else if (cardType.equals(CardType.MAESTRO_INTERNATIONAL)) {
            codeCardType = MAESTRO_INTERNATIONAL;
        } else {
            codeCardType = GE_MONEY_UK_CARD;
        }
        return codeCardType;
    }

    /**
     * Convert to bank name from card type ID.
     *
     * @param cardTypeId
     * @return
     */
    public static String convertBankNameFromCardTypeId(String cardTypeId) {
        String bankName = "";

        if (VISA.equalsIgnoreCase(cardTypeId)) {
            bankName = VISA_STRING;
        } else if (MASTERCARD.equalsIgnoreCase(cardTypeId)) {
            bankName = MASTERCARD_STRING;
        } else if (AMERICAN_EXPRESS.equalsIgnoreCase(cardTypeId)) {
            bankName = AMERICAN_EXPRESS_STRING;
        } else if (DISCOVER.equalsIgnoreCase(cardTypeId)) {
            bankName = DISCOVER_STRING;
        } else if (DINERS_CLUB.equalsIgnoreCase(cardTypeId)) {
            bankName = DINERS_CLUB_STRING;
        } else if (CARTE_BLANCHE.equalsIgnoreCase(cardTypeId)) {
            bankName = CARTE_BLANCHE_STRING;
        } else if (JCB.equalsIgnoreCase(cardTypeId)) {
            bankName = JCB_STRING;
        } else if (ENROUTE.equalsIgnoreCase(cardTypeId)) {
            bankName = ENROUTE_STRING;
        } else if (JAL.equalsIgnoreCase(cardTypeId)) {
            bankName = JAL_STRING;
        } else if (DELTA.equalsIgnoreCase(cardTypeId)) {
            bankName = DELTA_STRING;
        } else if (VISA_ELECTRON.equalsIgnoreCase(cardTypeId)) {
            bankName = VISA_ELECTRON_STRING;
        } else if (DANKORT.equalsIgnoreCase(cardTypeId)) {
            bankName = DANKORT_STRING;
        } else if (CARTE_BLEUE.equalsIgnoreCase(cardTypeId)) {
            bankName = CARTE_BLEUE_STRING;
        } else if (CARTA_SI.equalsIgnoreCase(cardTypeId)) {
            bankName = CARTA_SI_STRING;
        } else if (MAESTRO_UK_DOMESTIC.equalsIgnoreCase(cardTypeId)) {
            bankName = MAESTRO_UK_DOMESTIC_STRING;
        } else if (MAESTRO_INTERNATIONAL.equalsIgnoreCase(cardTypeId)) {
            bankName = MAESTRO_INTERNATIONAL_STRING;
        } else {
            bankName = GE_MONEY_UK_CARD_STRING;
        }
        return bankName;
    }

}
